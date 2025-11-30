package com.uit.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories; // Import this

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
        "com.uit.userservice",
        "com.uit.sharedkernel" // để GlobalExceptionHandler được scan
})// Add this line
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}