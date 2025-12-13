package com.uit.accountservice.service;

import com.uit.accountservice.config.RabbitMQConfig;
import com.uit.accountservice.dto.BalanceChangeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitMQPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publish(BalanceChangeMessage dto) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.NOTIF_TRANSFER_CREATED_KEY,
                dto
        );
        System.out.println("Sent message: " + dto);
    }
}
