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
			if (accountRepo.count()==0) {
				accountRepo.save(new Account("alice_phone",new BigDecimal("5000.0")));
				accountRepo.save(new Account("bob_phone",new BigDecimal("5000.0")));
				System.out.println("Seeded Alice and Bob with ₹5,000 each.");
			}

			// try {
			// 	System.out.println("Attempting to transfer 200 from alice to bob..");
			// 	settlementService.processPayment("alice_phone", "bob_phone", new BigDecimal("200.00"), "test_001");
			// 	System.out.println("Transaction Succesfull...");
			// } catch (Exception e) {
			// 	System.out.println("Transfer Failed: "+e.getMessage());
			// }
		};
	}

}

