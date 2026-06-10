package com.example.UPImesh.service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {//This is your idempotency check. Its job is to make sure the same packet is processed only once, even if multiple bridge nodes upload it.
    // the time to live TTL
    private final ConcurrentHashMap<String,Instant> seenHases =new ConcurrentHashMap<>(); //we are using Concurrent Hashmap bevause its putIfAbsent method is atomic ven if 10 threads hit at the exact same nanosecond one one succeds
    public boolean claim(String packetHash){
        Instant now = Instant.now();
        Instant previous=seenHases.putIfAbsent(packetHash, now);
        return previous==null;
    }
    // claim() atomically records a packet hash and returns true only for the first thread that sees that hash, preventing duplicate processing of the same payment packet.
}

