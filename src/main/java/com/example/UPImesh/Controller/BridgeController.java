package com.example.UPImesh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.example.UPImesh.model.MeshPacket;
import com.example.UPImesh.service.BridgeIngestionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
@RequestMapping("/api/bridge")
public class BridgeController {
    @Autowired
    private BridgeIngestionService bridgeIngestionService;
    
    @PostMapping("/ingest")
    public ResponseEntity<String> ingestPacket(@RequestBody MeshPacket packet) {
        return ResponseEntity.ok(bridgeIngestionService.ingest(packet));
    }
    
}

