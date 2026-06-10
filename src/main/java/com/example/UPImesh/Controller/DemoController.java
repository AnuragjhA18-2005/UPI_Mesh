package com.example.UPImesh.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.UPImesh.crypto.HybridCryptoService;
import com.example.UPImesh.crypto.ServerKeyHolder;
import com.example.UPImesh.model.MeshPacket;
import com.example.UPImesh.model.PaymentInstruction;

@RestController
@RequestMapping("/api/demo")
public class DemoController {
    private final HybridCryptoService hybridCryptoService;
    private final ServerKeyHolder serverKeyHolder;

    public DemoController(HybridCryptoService hybridCryptoService, ServerKeyHolder serverKeyHolder) {
        this.hybridCryptoService = hybridCryptoService;
        this.serverKeyHolder = serverKeyHolder;
    }

    @GetMapping("/generate-packet")
    public ResponseEntity<MeshPacket> generateTestPacket() throws Exception{
        PaymentInstruction instruction = new PaymentInstruction();
        instruction.setSenderID("alice_phone"); // Assuming Alice is still in your DB
        instruction.setReceiverID("bob_phone");
        instruction.setAmount(new BigDecimal("100.00"));
        instruction.setNonce(UUID.randomUUID().toString());
        instruction.setSignedAt(Instant.now().toEpochMilli());

        String cipherText=hybridCryptoService.encrypt(instruction, serverKeyHolder.getPublicKey());

        MeshPacket packet = new MeshPacket();
        packet.setPacketId(UUID.randomUUID().toString());
        packet.setCipherText(cipherText);

        return ResponseEntity.ok(packet);
    }
}

