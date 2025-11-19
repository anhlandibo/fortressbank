package com.uit.referenceservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaRepositories("com.uit.referenceservice.repository")
public class ReferenceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReferenceServiceApplication.class, args);
	}

}

