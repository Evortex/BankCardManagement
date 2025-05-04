package com.example.bankcardmanagement.entity;

import com.example.bankcardmanagement.enums.CardStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.YearMonth;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cards", indexes = {
        @Index(name = "idx_card_owner_id", columnList = "owner_id"),
        @Index(name = "idx_card_status", columnList = "status")
})
public class Card extends BaseEntity {

    @Column(name = "card_number_encrypted", nullable = false, length = 255)
    private String cardNumberEncrypted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @NotNull
    private User owner;

    @Column(name = "card_holder_name", nullable = false)
    private String cardHolderName;

    @Column(name = "expiry_date", nullable = false, columnDefinition = "VARCHAR(7)")
    @Convert(converter = YearMonthAttributeConverter.class)
    private YearMonth expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private CardStatus status = CardStatus.ACTIVE;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    public Card(String cardNumberEncrypted, User owner, String cardHolderName, YearMonth expiryDate) {
        this.cardNumberEncrypted = cardNumberEncrypted;
        this.owner = owner;
        this.cardHolderName = cardHolderName;
        this.expiryDate = expiryDate;
        this.status = CardStatus.ACTIVE;
        this.balance = BigDecimal.ZERO;
    }

    public boolean isActive() {
        return this.status == CardStatus.ACTIVE && YearMonth.now().isBefore(this.expiryDate);
    }

    public void checkAndUpdateExpiryStatus() {
        if (this.status != CardStatus.BLOCKED && YearMonth.now().isAfter(this.expiryDate)) {
            this.status = CardStatus.EXPIRED;
        }
    }
}