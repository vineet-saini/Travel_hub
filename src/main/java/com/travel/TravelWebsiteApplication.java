package com.travel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TravelWebsiteApplication {

	public static void main(String[] args) {
		SpringApplication.run(TravelWebsiteApplication.class, args);
	}
}
