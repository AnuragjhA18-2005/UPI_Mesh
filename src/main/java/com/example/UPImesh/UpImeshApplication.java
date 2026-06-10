package com.example.UPImesh;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.UPImesh.model.Account;
import com.example.UPImesh.repositories.AccountRepo;
import com.example.UPImesh.service.SettlementService;

@SpringBootApplication
@EnableScheduling
public class UpImeshApplication {

	public static void main(String[] args) {
		SpringApplication.run(UpImeshApplication.class, args);
	}
	@Bean
	public CommandLineRunner testRunner(AccountRepo accountRepo,SettlementService settlementService){
		return args ->{
			// Seed test accounts for the prototype
			saveOrUpdateAccount(accountRepo, "alice@upimesh", new BigDecimal("5000.0"));
			saveOrUpdateAccount(accountRepo, "bob@upimesh", new BigDecimal("5000.0"));
			saveOrUpdateAccount(accountRepo, "charlie@upimesh", new BigDecimal("5000.0"));
			System.out.println("Prototype Environment Seeded: Alice, Bob, and Charlie set to ₹5,000.");
		};
	}

	private void saveOrUpdateAccount(AccountRepo accountRepo, String accountId, BigDecimal balance) {
		Account account = accountRepo.findById(accountId).orElse(new Account(accountId, balance));
		account.setBalance(balance);
		accountRepo.save(account);
	}

}

