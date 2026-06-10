package com.example.UPImesh.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component //"Create an object of this class automatically and manage it as a Spring Bean." so instead of ServerKeyHolder holder = new ServerKeyHolder(); we can simply inject it with autowire
public class ServerKeyHolder {
    private KeyPair keyPair;

    @PostConstruct //"Run this method (init()) automatically once after the bean is created and all dependencies are injected." no need to call init() ourselves once ServerKeyHolder is created this method is called automatically
    public void init() throws NoSuchAlgorithmException{
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048); //This sets the RSA key size to 2048 bits. more bits is more secure 
        this.keyPair=gen.genKeyPair();
        System.out.println("The RSA key Pair Succesfully Created");
    }

    public PublicKey getPublicKey(){
        return keyPair.getPublic();
    }
    public PrivateKey getPrivateKey(){
        return keyPair.getPrivate();
    }
}

