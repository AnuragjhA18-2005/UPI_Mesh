package com.example.UPImesh.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.example.UPImesh.crypto.HybridCryptoService;
import com.example.UPImesh.model.MeshPacket;
import com.example.UPImesh.model.PaymentInstruction;
import com.example.UPImesh.model.Transaction;

@Service
public class BridgeIngestionService {
    private static final Logger log = LoggerFactory.getLogger(BridgeIngestionService.class);

    private final HybridCryptoService hybridCryptoService;
    private final IdempotencyService idempotencyService;
    private final SettlementService settlementService;

    public BridgeIngestionService(HybridCryptoService hybridCryptoService, 
                                 IdempotencyService idempotencyService, 
                                 SettlementService settlementService) {
        this.hybridCryptoService = hybridCryptoService;
        this.idempotencyService = idempotencyService;
        this.settlementService = settlementService;
    }

    private static final long MAX_AGE_MILLIS = 24 * 60 * 60 * 1000;

    public String ingest(MeshPacket packet) {
        MDC.put("packetId", packet.getPacketId());
        try {
            log.info("Starting ingestion for packet");

            String packetHash = hybridCryptoService.hashCipherText(packet.getCipherText());
            if (!idempotencyService.claim(packetHash)) {
                log.warn("Duplicate packet dropped. Hash: {}", packetHash);
                throw new IllegalArgumentException("DUPLICATE_PACKET_DROPPED");
            }

            log.debug("Packet hash claimed, proceeding to decryption");
            
            PaymentInstruction instruction;
            if (packet.getPacketId().startsWith("offline-")) {
                log.info("Demo Mode: Processing simulated offline packet");
                String decoded = new String(java.util.Base64.getDecoder().decode(packet.getCipherText()));
                
                // Use Jackson to parse the mock JSON from the simulated packet
                com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(decoded);
                
                instruction = new PaymentInstruction();
                instruction.setSenderID(node.has("sender") ? node.get("sender").asText() : "alice@upimesh");
                instruction.setReceiverID(node.get("receiver").asText());
                instruction.setAmount(new java.math.BigDecimal(node.get("amount").asText()));
                instruction.setSignedAt(node.has("timestamp") ? node.get("timestamp").asLong() : Instant.now().toEpochMilli());
            } else {
                instruction = hybridCryptoService.decrypt(packet.getCipherText());
            }

            long ageMillis = Instant.now().toEpochMilli() - instruction.getSignedAt();
            if (ageMillis > MAX_AGE_MILLIS) {
                log.warn("Stale packet rejected. Age: {}ms", ageMillis);
                throw new IllegalArgumentException("STALE_PACKET_REJECTED");
            }

            log.info("Instruction decrypted successfully for sender: {}", instruction.getSenderID());

            Transaction ts = settlementService.processPayment(
                instruction.getSenderID(),
                instruction.getReceiverID(),
                instruction.getAmount(),
                packet.getPacketId()
            );

            log.info("Transaction settled successfully. Transaction ID: {}", ts.getId());
            return "SETTLED:Transaction ID " + ts.getId();

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Re-throw these to be handled by GlobalExceptionHandler
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during packet ingestion: ", e);
            throw new RuntimeException("Error processing packet: " + e.getMessage(), e);
        } finally {
            MDC.remove("packetId");
        }
    }
}

