package com.uit.accountservice.service;

import com.uit.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    // TODO: Implement the business logic from the secureBank demo
}
