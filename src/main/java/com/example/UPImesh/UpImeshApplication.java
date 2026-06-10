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
			// Force reset Alice and Bob for the demo
			accountRepo.save(new Account("alice_phone",new BigDecimal("5000.0")));
			accountRepo.save(new Account("bob_phone",new BigDecimal("5000.0")));
			System.out.println("Demo Environment Reset: Alice and Bob set to ₹5,000.");
		};
	}

}

