package com.example.UPImesh.crypto;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.example.UPImesh.model.PaymentInstruction;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class HybridCryptoService {
    private final ServerKeyHolder serverKeyHolder;

    public HybridCryptoService(ServerKeyHolder serverKeyHolder) {
        this.serverKeyHolder = serverKeyHolder;
    }

    private final ObjectMapper json= new ObjectMapper();//used to convert java object to json bytes
    private final SecureRandom rng = new SecureRandom();

    private static final String RSA_TRANS = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";// Defines encryption scehme for RSA part of system "RSA" algortihm used for assymetric encryption "ECB" means we are encrypting without chaining  "OAEPWithSHA-256AndMGF1Padding" OAEP(Optimal Asymmetric encryption padding) its the modern standard "MGF1" this makes sure that encrypting the same AES key twice results ina totally different encryption
    private static final String AES_TRANS = "AES/GCM/NoPadding";//defines the encryption scheme for AES part "AES" Advanced encryption standard "GCM" is an authenticated encryption mode it ensures confidentiality(hiding data) and integirty(making sure data hasnt been changed) it creates an authentication tag if a malicious user tampers with even one bit of cipher text GCM fails to verify it and decrypt() throws exception 
    private static final int RSA_KEY_BYTES = 256; // 2048-bit RSA outputs 256 bytes

    //Decrypts the incoming locked box 
    public PaymentInstruction decrypt(String base64Ciphertext) throws Exception{
        byte[] all = Base64.getDecoder().decode(base64Ciphertext);

        if (all.length < RSA_KEY_BYTES + 12) {
            throw new IllegalArgumentException("Invalid ciphertext: length is too short to contain key, IV, and data");
        }

        //Unpacking the Byte Array

        byte[] encryptedAESkey=new byte[RSA_KEY_BYTES];
        byte[] iv = new byte[12];
        byte[] AEScipherText=new byte[all.length -RSA_KEY_BYTES-12];

        ByteBuffer buf = ByteBuffer.wrap(all);
        buf.get(encryptedAESkey);
        buf.get(iv);
        buf.get(AEScipherText);

        //RSA decrypts the AES key
        Cipher rsa = Cipher.getInstance(RSA_TRANS);
        OAEPParameterSpec oaep = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
        rsa.init(Cipher.DECRYPT_MODE, serverKeyHolder.getPrivateKey(), oaep);
        // Put RSA cipher into decrypt mode Use the server's private key
        SecretKey aesKey = new SecretKeySpec(rsa.doFinal(encryptedAESkey), "AES");
        // Takes the encrypted AES key bytes,Uses the RSA private key to decrypt them,Produces the original AES key bytes ,Secret key object has decrypted AES 

        //AES-GCM decrypts the payment payload (Actual Payment data )
        Cipher aes=Cipher.getInstance(AES_TRANS);
        aes.init(Cipher.DECRYPT_MODE, aesKey,new GCMParameterSpec(128, iv));
        byte[] plaintext=aes.doFinal(AEScipherText);

        return json.readValue(plaintext,PaymentInstruction.class);
    }
    //Hashing the ciphertext for idempotency check
    public String hashCipherText(String base64Ciphertext) throws Exception{
        MessageDigest sha256= MessageDigest.getInstance("SHA-256");
        byte[] hash=sha256.digest(base64Ciphertext.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
    public String encrypt(PaymentInstruction instruction,java.security.PublicKey serPublicKey) throws Exception{
        byte[] plaintext=json.writeValueAsBytes(instruction);


        //genrating one time AES key

        KeyGenerator kg= KeyGenerator.getInstance("AES");
        kg.init(256);
        SecretKey aeskey=kg.generateKey();

        //AES-GCM encrypt the payload
        byte[] iv=new byte[12];
        rng.nextBytes(iv);
        Cipher aes=Cipher.getInstance(AES_TRANS);
        aes.init(Cipher.ENCRYPT_MODE, aeskey,new GCMParameterSpec(128, iv));
        byte[] aesCiphertext= aes.doFinal(plaintext);

        // 3. RSA-OAEP encrypt the AES key
        Cipher rsa = Cipher.getInstance(RSA_TRANS);
        OAEPParameterSpec oaep = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
        rsa.init(Cipher.ENCRYPT_MODE, serPublicKey, oaep);
        byte[] encryptedAesKey = rsa.doFinal(aeskey.getEncoded());

        //combine into a single byte array
        ByteBuffer buf = ByteBuffer.allocate(encryptedAesKey.length +iv.length + aesCiphertext.length);
        buf.put(encryptedAesKey);
        buf.put(iv);
        buf.put(aesCiphertext);

        //encode to base 64
        return Base64.getEncoder().encodeToString(buf.array());
    }
}

