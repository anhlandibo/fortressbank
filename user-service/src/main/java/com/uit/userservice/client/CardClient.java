package com.uit.userservice.client;

import com.uit.sharedkernel.api.ApiResponse;
import com.uit.userservice.dto.response.CardDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "account-service", contextId = "cardClient")
public interface CardClient {

    @PostMapping("/cards/internal/account/{accountId}/issue")
    ApiResponse<CardDto> issueCard(
            @PathVariable("accountId") String accountId,
            @RequestParam("fullName") String fullName
    );
}
