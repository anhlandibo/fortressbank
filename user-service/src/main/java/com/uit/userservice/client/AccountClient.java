package com.uit.userservice.client;

import com.uit.sharedkernel.api.ApiResponse;
import com.uit.userservice.dto.response.AccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "account-service")
public interface AccountClient {

    @PostMapping("/accounts/internal/create/{userId}")
    ApiResponse<AccountDto> createAccountForUser(
            @PathVariable("userId") String userId,
            @RequestBody CreateAccountInternalRequest request
    );
}
