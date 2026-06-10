package com.example.UPImesh.model;

import java.math.BigDecimal;

public class PaymentInstruction {
    private String senderID;
    private String receiverID;
    private BigDecimal amount;
    private String nonce;// UUID this is unique for each payment to make sure to replay attack happens 
    private long signedAt;

    public PaymentInstruction() {
    }

    public PaymentInstruction(String senderID, String receiverID, BigDecimal amount, String nonce, long signedAt) {
        this.senderID = senderID;
        this.receiverID = receiverID;
        this.amount = amount;
        this.nonce = nonce;
        this.signedAt = signedAt;
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

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public long getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(long signedAt) {
        this.signedAt = signedAt;
    }
}

