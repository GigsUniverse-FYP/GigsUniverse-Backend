package com.giguniverse.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GigsuniverseBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GigsuniverseBackendApplication.class, args);
	}

}
