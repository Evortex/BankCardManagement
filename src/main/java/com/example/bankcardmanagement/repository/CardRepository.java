package com.example.bankcardmanagement.repository;

import com.example.bankcardmanagement.entity.Card;
import com.example.bankcardmanagement.enums.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    Page<Card> findByOwnerId(Long ownerId, Pageable pageable);

    Optional<Card> findByIdAndOwnerId(Long cardId, Long ownerId);

    Optional<Card> findByCardNumberEncrypted(String cardNumberEncrypted);

    @Query("SELECT c FROM Card c WHERE c.owner.id = :ownerId AND (:status is null OR c.status = :status)")
    Page<Card> findByOwnerIdAndStatus(@Param("ownerId") Long ownerId,
                                      @Param("status") CardStatus status,
                                      Pageable pageable);

    //@Query("SELECT c FROM Card c WHERE c.expiryDate < CURRENT_DATE AND c.status = 'ACTIVE'")
    List<Card> findExpiredActiveCards();

}
