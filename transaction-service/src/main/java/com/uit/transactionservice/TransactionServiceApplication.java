package com.uit.transactionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@ComponentScan(basePackages = {
        "com.uit.transactionservice",
        "com.uit.sharedkernel" // để GlobalExceptionHandler được scan
})// Add this line
public class TransactionServiceApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .directory("./") // Đường dẫn tới file .env (thư mục gốc)
                .ignoreIfMissing() // Để khi deploy lên server thật (không có file .env) thì không bị lỗi
                .load();

        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
