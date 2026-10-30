package com.clothingstore.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClothingShopApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClothingShopApplication.class, args);
	}

}
