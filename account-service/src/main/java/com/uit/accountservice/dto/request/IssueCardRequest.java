package com.uit.accountservice.dto.request;

public record IssueCardRequest(
        String accountId,
        String cardType // PHYSICAL or VIRTUAL
) {}