package com.example.bankcardmanagement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {
    @NotNull(message = "ID карты-отправителя не может быть пустым")
    private Long fromCardId;

    @NotNull(message = "ID карты-получателя не может быть пустым")
    private Long toCardId;

    @NotNull(message = "Сумма перевода не может быть пустой")
    @DecimalMin(value = "0.01", message = "Сумма перевода должна быть положительной")
    private BigDecimal amount;
}
