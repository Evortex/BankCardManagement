package com.example.bankcardmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BankCardManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankCardManagementApplication.class, args);
    }

}
