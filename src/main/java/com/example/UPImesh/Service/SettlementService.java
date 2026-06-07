package com.example.UPImesh.Service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.UPImesh.Model.Account;
import com.example.UPImesh.Model.Transaction;
import com.example.UPImesh.Repositories.AccountRepo;
import com.example.UPImesh.Repositories.TransactionRepo;

import jakarta.transaction.Transactional;

@Service
public class SettlementService {
    @Autowired
    private AccountRepo accountrepo;

    @Autowired
    private TransactionRepo transactionrepo;

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

        if (transactionrepo.existsByPacketId(packetId)) {
            throw new IllegalArgumentException("Packet already processed");
        }
        Account sender = accountrepo.findById(senderID)
                .orElseThrow(() -> new IllegalArgumentException("Sender Account Not Found"));
        Account receiver = accountrepo.findById(receiverID)
                .orElseThrow(() -> new IllegalArgumentException("Receiver Account Not Found"));

        if (sender.getBalance() == null || receiver.getBalance() == null) {
            throw new IllegalStateException("Account balance not initialized");
        }
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient Balance");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        accountrepo.save(sender);
        accountrepo.save(receiver);

        Transaction transaction = new Transaction(0, senderID, receiverID, amount, Instant.now(), packetId);
        return transactionrepo.save(transaction);
    }

}
