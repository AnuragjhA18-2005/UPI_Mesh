package com.example.UPImesh.Service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.UPImesh.Crypto.HybridCryptoService;
import com.example.UPImesh.Model.MeshPacket;
import com.example.UPImesh.Model.PaymentInstruction;
import com.example.UPImesh.Model.Transaction;

@Service
// this service ties together the Settlement an idempotency Services
public class BridgeIngestionService {
    @Autowired
    private HybridCryptoService hybridCryptoService;
    @Autowired
    private IdempotencyService idempotencyService;
    @Autowired
    private SettlementService settlementService;

    // 24 hours in milliseconds
    private static final long MAX_AGE_MILLIS = 24 * 60 * 60 * 1000;
    public String ingest(MeshPacket packet){
        try {
            // hash the cipher text
            String packetHash=hybridCryptoService.hashCipherText(packet.getCipherText());
            if (!idempotencyService.claim(packetHash)) {
                return "DUPLICATE_DROPPED";
            }
            PaymentInstruction instruction=hybridCryptoService.decrypt(packet.getCipherText());
            long ageMillis = Instant.now().toEpochMilli() - instruction.getSignedAt();
            if (ageMillis > MAX_AGE_MILLIS) {
                return "INVALID_STALE_PACKET";
            }
            // settle the payment in database
            Transaction ts=settlementService.processPayment(instruction.getSenderID(),instruction.getReceiverID(), instruction.getAmount(),packet.getPacketId());
            return "SETTLED:Transaction ID "+ts.getId();

        } catch (Exception e) {
            return "INVALID"+e.getMessage();
        }
    }
}
