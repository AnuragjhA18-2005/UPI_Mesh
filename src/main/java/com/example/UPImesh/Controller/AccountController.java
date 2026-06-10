package com.example.UPImesh.controller;

import java.util.Map;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.UPImesh.repositories.AccountRepo;
import com.example.UPImesh.repositories.TransactionRepo;
import com.example.UPImesh.model.Transaction;

import com.example.UPImesh.model.Account;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepo accountRepo;
    private final TransactionRepo transactionRepo;

    public AccountController(AccountRepo accountRepo, TransactionRepo transactionRepo) {
        this.accountRepo = accountRepo;
        this.transactionRepo = transactionRepo;
    }

    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        return ResponseEntity.ok(accountRepo.findAll());
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<?> getBalance(@PathVariable String id) {
        return accountRepo.findById(id)
                .map(account -> ResponseEntity.ok(Map.of("id", id, "balance", account.getBalance())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<Transaction>> getTransactions(@PathVariable String id) {
        return ResponseEntity.ok(transactionRepo.findBySenderIDOrReceiverIDOrderByTimestampDesc(id, id));
    }
}
