package com.example.UPImesh.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component // makes JWTutil object ready for injection in other classes via @Autowired
public class JWTutil {
    private final Key key;
    private final long EXPIRATION_TIME=1000*60*60*24; // 24 hrs

    public JWTutil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String bridgeId){
        return Jwts.builder().setSubject(bridgeId).setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)).signWith(key).compact();
    }
    //setSubject(bridgeId) stores bridgeId inside the token ,.signWith(key) signs it with the secret key we generated .compac() makes it into a JWT string

    public String extractBridgeId(String token){
        return getClaims(token).getSubject();
    }
    public Boolean validateToken(String token){
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

}

