package com.example.UPImesh.service;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.example.UPImesh.model.Account;
import com.example.UPImesh.model.Transaction;
import com.example.UPImesh.repositories.AccountRepo;
import com.example.UPImesh.repositories.TransactionRepo;
import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SettlementServiceTest {

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private AccountRepo accountRepo;

    @Autowired
    private TransactionRepo transactionRepo;

    private final String SENDER_ID = "sender@upimesh";
    private final String NEW_RECEIVER_ID = "newuser@upimesh";

    @BeforeEach
    void setUp() {
        transactionRepo.deleteAll();
        accountRepo.deleteAll();
        accountRepo.save(new Account(SENDER_ID, new BigDecimal("1000.00")));
    }

    @Test
    void testProcessPaymentWithAutoDiscovery() {
        BigDecimal amount = new BigDecimal("100.00");
        String packetId = "test-packet-1";
        String nonce = "nonce-1";

        // Process payment to a non-existent account
        Transaction tx = settlementService.processPayment(SENDER_ID, NEW_RECEIVER_ID, amount, packetId, nonce);

        assertNotNull(tx);
        assertEquals(SENDER_ID, tx.getSenderID());
        assertEquals(NEW_RECEIVER_ID, tx.getReceiverID());
        assertEquals(amount, tx.getAmount());
        assertEquals(nonce, tx.getNonce());

        // Verify accounts
        Account sender = accountRepo.findById(SENDER_ID).orElseThrow();
        Account receiver = accountRepo.findById(NEW_RECEIVER_ID).orElseThrow();

        assertEquals(new BigDecimal("900.00"), sender.getBalance());
        assertEquals(new BigDecimal("100.00"), receiver.getBalance());
    }

    @Test
    void testTransactionSyncFetch() {
        BigDecimal amount = new BigDecimal("50.00");
        
        // 1. Sender pays Receiver
        settlementService.processPayment(SENDER_ID, NEW_RECEIVER_ID, amount, "p1", "n1");
        
        // 2. Someone else pays Sender (Sender becomes receiver)
        String otherUser = "other@upimesh";
        accountRepo.save(new Account(otherUser, new BigDecimal("500.00")));
        settlementService.processPayment(otherUser, SENDER_ID, new BigDecimal("20.00"), "p2", "n2");

        // Fetch transactions for SENDER_ID
        List<Transaction> txs = transactionRepo.findBySenderIDOrReceiverIDOrderByTimestampDesc(SENDER_ID, SENDER_ID);

        assertEquals(2, txs.size(), "Should find both sent and received transactions");
        
        // Latest first
        assertEquals("p2", txs.get(0).getPacketId());
        assertEquals(SENDER_ID, txs.get(0).getReceiverID());
        
        assertEquals("p1", txs.get(1).getPacketId());
        assertEquals(SENDER_ID, txs.get(1).getSenderID());
    }
}
