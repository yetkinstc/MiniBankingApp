package com.miniBankingApp.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDTO {
	private UUID id; 
    private String number;
    private String name;
    private BigDecimal balance;
    private String username;
}