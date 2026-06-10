package com.example.UPImesh.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String senderID;
    private String receiverID;
    private BigDecimal amount;
    private Instant timestamp;
    private String packetId;// to track which offline packet caused this 

    public Transaction() {}

    public Transaction(long id, String senderID, String receiverID, BigDecimal amount, Instant timestamp,
            String packetId) {
        this.id = id;
        this.senderID = senderID;
        this.receiverID = receiverID;
        this.amount = amount;
        this.timestamp = timestamp;
        this.packetId = packetId;
    }

     public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSenderID() {
        return senderID;
    }

    public void setSenderID(String senderID) {
        this.senderID = senderID;
    }

    public String getReceiverID() {
        return receiverID;
    }

    public void setReceiverID(String receiverID) {
        this.receiverID = receiverID;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getPacketId() {
        return packetId;
    }

    public void setPacketId(String packetId) {
        this.packetId = packetId;
    }

}

