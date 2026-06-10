package com.example.UPImesh.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.UPImesh.model.Account;
import com.example.UPImesh.model.Transaction;
import com.example.UPImesh.repositories.AccountRepo;
import com.example.UPImesh.repositories.TransactionRepo;

import jakarta.transaction.Transactional;

@Service
public class SettlementService {
    private final AccountRepo accountRepo;
    private final TransactionRepo transactionRepo;

    public SettlementService(AccountRepo accountRepo, TransactionRepo transactionRepo) {
        this.accountRepo = accountRepo;
        this.transactionRepo = transactionRepo;
    }

    @Transactional // it ensures that if the app crashes right after sender sends the money and before it reaches receivers ,the database transaction rolls back and no one loses money
    // @Transactional makes a group of database operations behave as a single unit—either all succeed and commit, or all fail and rollback.
    public Transaction processPayment(String senderID, String receiverID, BigDecimal amount, String packetId) {
        if (senderID == null || senderID.isBlank()) {
            throw new IllegalArgumentException("Sender ID must not be null or blank");
        }
        if (receiverID == null || receiverID.isBlank()) {
            throw new IllegalArgumentException("Receiver ID must not be null or blank");
        }
        if (senderID.equals(receiverID)) {
            throw new IllegalArgumentException("Sender and Receiver cannot be the same");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (packetId == null || packetId.isBlank()) {
            throw new IllegalArgumentException("Packet ID must not be null or blank");
        }

        if (transactionRepo.existsByPacketId(packetId)) {
            throw new IllegalArgumentException("Packet already processed");
        }
        Account sender = accountRepo.findById(senderID)
                .orElseThrow(() -> new IllegalArgumentException("Sender Account Not Found"));
        
        // Auto-Discovery: If receiver doesn't exist, create it with ₹0
        Account receiver = accountRepo.findById(receiverID)
                .orElseGet(() -> {
                    log.info("Auto-discovery: Creating new account for {}", receiverID);
                    return accountRepo.save(new Account(receiverID, BigDecimal.ZERO));
                });

        if (sender.getBalance() == null || receiver.getBalance() == null) {
            throw new IllegalStateException("Account balance not initialized");
        }
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient Balance");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        accountRepo.save(sender);
        accountRepo.save(receiver);

        Transaction transaction = new Transaction(0, senderID, receiverID, amount, Instant.now(), packetId);
        return transactionRepo.save(transaction);
    }

}

