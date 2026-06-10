package com.example.UPImesh.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.UPImesh.repositories.AccountRepo;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepo accountRepo;

    public AccountController(AccountRepo accountRepo) {
        this.accountRepo = accountRepo;
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<?> getBalance(@PathVariable String id) {
        return accountRepo.findById(id)
                .map(account -> ResponseEntity.ok(Map.of("id", id, "balance", account.getBalance())))
                .orElse(ResponseEntity.notFound().build());
    }
}
