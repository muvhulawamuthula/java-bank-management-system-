package com.bankafrica.bankingapp;



import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BankingappApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankingappApplication.class, args);
		System.out.println("🏛️ Bank Africa Application Started Successfully!");
		System.out.println("🌐 Frontend: http://localhost:8080");
		System.out.println("🔗 API Base URL: http://localhost:8080/api");
	}
}