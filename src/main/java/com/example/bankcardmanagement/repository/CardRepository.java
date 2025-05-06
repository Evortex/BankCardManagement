package com.example.bankcardmanagement.repository;

import com.example.bankcardmanagement.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    Optional<Card> findByIdAndOwnerId(Long cardId, Long ownerId);

    Optional<Card> findByCardNumberEncrypted(String cardNumberEncrypted);
}
