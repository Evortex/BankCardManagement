package com.example.bankcardmanagement.dto.response;

import com.example.bankcardmanagement.enums.CardStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.LocalDateTime;

@Data
public class CardResponse {
    private Long id;
    private String maskedCardNumber;
    private String cardHolderName;
    private YearMonth expiryDate;
    private CardStatus status;
    private BigDecimal balance;
    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
