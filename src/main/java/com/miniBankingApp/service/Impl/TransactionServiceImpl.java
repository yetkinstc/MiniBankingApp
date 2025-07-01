package com.miniBankingApp.service.Impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniBankingApp.DTO.TransactionDTO;
import com.miniBankingApp.DTO.UserDTO;
import com.miniBankingApp.entity.Account;
import com.miniBankingApp.entity.Transaction;
import com.miniBankingApp.entity.TransactionStatus;
import com.miniBankingApp.entity.User;
import com.miniBankingApp.exception.NotFoundException;
import com.miniBankingApp.repository.AccountRepository;
import com.miniBankingApp.repository.TransactionRepository;
import com.miniBankingApp.repository.UserRepository;
import com.miniBankingApp.service.ITransactionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements ITransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionMapping transactionMapping; // Mapping DTO <-> Entity

    @Override
    @Transactional
    public TransactionDTO transferMoney(UUID fromAccountId, UUID toAccountId, BigDecimal amount, UserDTO userDto) {
        if(amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer miktarı 0'dan büyük olmalı");
        }

        User user = userRepository.findById(userDto.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new NotFoundException("Gönderen hesabı bulunamadı"));

        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new NotFoundException("Alıcı hesabı bulunamadı"));

        // Hesap sahibini kontrol et
        if(!fromAccount.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bu hesaba erişim yetkiniz yok");
        }

        // Bakiye kontrolü
        if(fromAccount.getBalance().compareTo(amount) < 0) {
        	Transaction failedTx = new Transaction();
            failedTx.setFrom(fromAccount);
            failedTx.setTo(toAccount);
            failedTx.setAmount(amount);
            failedTx.setTransactionDate(LocalDateTime.now());
            failedTx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(failedTx);

            throw new RuntimeException("Yetersiz bakiye");
        }

        // İşlem başlatılıyor
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = new Transaction();
        transaction.setFrom(fromAccount);
        transaction.setTo(toAccount);
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(TransactionStatus.SUCCESS);

        Transaction savedTx = transactionRepository.save(transaction);

        return transactionMapping.toDto(savedTx);
    }

    @Override
    public List<TransactionDTO> getTransactionHistory(UUID accountId, UserDTO userDto) {
        User user = userRepository.findById(userDto.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Hesap bulunamadı"));

        if(!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bu hesaba erişim yetkiniz yok");
        }

        List<Transaction> transactions = transactionRepository.findByFromOrTo(account, account);

        return transactions.stream()
                .map(transactionMapping::toDto)
                .collect(Collectors.toList());
    }
}
