package com.example.UPImesh.Model;

public class MeshPacket {
    private String packetId; // uuid to track the packet in the mesh network
    private String cipherText; // base 64 string containing the rsa encrypted aes key and the aes-gcm encrypted payment instruction
    public MeshPacket(){

    }
    
    public MeshPacket(String packetId, String cipherText) {
        this.packetId = packetId;
        this.cipherText = cipherText;
    }

    
    public String getPacketId() {
        return packetId;
    }

    public void setPacketId(String packetId) {
        this.packetId = packetId;
    }

    public String getCipherText() {
        return cipherText;
    }

    public void setCipherText(String cipherText) {
        this.cipherText = cipherText;
    } 
}
