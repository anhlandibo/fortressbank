package com.uit.transactionservice.client;

import com.uit.transactionservice.client.dto.AccountBalanceRequest;
import com.uit.transactionservice.client.dto.AccountBalanceResponse;
import com.uit.transactionservice.client.dto.InternalTransferRequest;
import com.uit.transactionservice.client.dto.InternalTransferResponse;
import com.uit.transactionservice.exception.AccountServiceException;
import com.uit.transactionservice.exception.InsufficientBalanceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Client for synchronous communication with Account Service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.account-service.url:http://localhost:4001}")
    private String accountServiceUrl;

    @Value("${services.account-service.timeout:5000}")
    private long timeout;

    /**
     * Debit (subtract) amount from an account
     * This is a synchronous blocking call with timeout
     */
    public AccountBalanceResponse debitAccount(String accountId, BigDecimal amount, String transactionId, String description) {
        String url = accountServiceUrl + "/accounts/" + accountId + "/debit";
        
        AccountBalanceRequest request = AccountBalanceRequest.builder()
                .accountId(accountId)
                .amount(amount)
                .transactionId(transactionId)
                .description(description)
                .build();

        log.info("Debiting account {} with amount {} for transaction {}", accountId, amount, transactionId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AccountBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<AccountBalanceResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    AccountBalanceResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully debited account {} - New balance: {}", accountId, response.getBody().getNewBalance());
                return response.getBody();
            } else {
                log.error("Unexpected response from account service: {}", response.getStatusCode());
                throw new AccountServiceException("Unexpected response from account service");
            }

        } catch (HttpClientErrorException e) {
            log.error("Client error while debiting account {}: {} - {}", accountId, e.getStatusCode(), e.getResponseBodyAsString());
            
            // Handle specific business errors
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST && e.getResponseBodyAsString().contains("Insufficient balance")) {
                throw new InsufficientBalanceException("Insufficient balance in account: " + accountId);
            } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new AccountServiceException("Account not found: " + accountId);
            } else if (e.getStatusCode() == HttpStatus.LOCKED) {
                throw new AccountServiceException("Account is locked: " + accountId);
            }
            
            throw new AccountServiceException("Failed to debit account: " + e.getMessage(), e);

        } catch (HttpServerErrorException e) {
            log.error("Server error from account service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AccountServiceException("Account service is temporarily unavailable", e);

        } catch (ResourceAccessException e) {
            log.error("Timeout or connection error while calling account service", e);
            throw new AccountServiceException("Cannot connect to account service - timeout", e);

        } catch (Exception e) {
            log.error("Unexpected error while debiting account", e);
            throw new AccountServiceException("Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Credit (add) amount to an account
     * This is a synchronous blocking call with timeout
     */
    public AccountBalanceResponse creditAccount(String accountId, BigDecimal amount, String transactionId, String description) {
        String url = accountServiceUrl + "/accounts/" + accountId + "/credit";
        
        AccountBalanceRequest request = AccountBalanceRequest.builder()
                .accountId(accountId)
                .amount(amount)
                .transactionId(transactionId)
                .description(description)
                .build();

        log.info("Crediting account {} with amount {} for transaction {}", accountId, amount, transactionId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AccountBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<AccountBalanceResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    AccountBalanceResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully credited account {} - New balance: {}", accountId, response.getBody().getNewBalance());
                return response.getBody();
            } else {
                log.error("Unexpected response from account service: {}", response.getStatusCode());
                throw new AccountServiceException("Unexpected response from account service");
            }

        } catch (HttpClientErrorException e) {
            log.error("Client error while crediting account {}: {} - {}", accountId, e.getStatusCode(), e.getResponseBodyAsString());
            
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new AccountServiceException("Account not found: " + accountId);
            } else if (e.getStatusCode() == HttpStatus.LOCKED) {
                throw new AccountServiceException("Account is locked: " + accountId);
            }
            
            throw new AccountServiceException("Failed to credit account: " + e.getMessage(), e);

        } catch (HttpServerErrorException e) {
            log.error("Server error from account service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AccountServiceException("Account service is temporarily unavailable", e);

        } catch (ResourceAccessException e) {
            log.error("Timeout or connection error while calling account service", e);
            throw new AccountServiceException("Cannot connect to account service - timeout", e);

        } catch (Exception e) {
            log.error("Unexpected error while crediting account", e);
            throw new AccountServiceException("Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Rollback debit operation (credit back the amount)
     * Used for compensating transaction in case of failure
     */
    public void rollbackDebit(String accountId, BigDecimal amount, String transactionId) {
        log.warn("Rolling back debit for account {} - amount: {} - transaction: {}", accountId, amount, transactionId);
        
        try {
            creditAccount(accountId, amount, transactionId, "Rollback debit for failed transaction");
        } catch (Exception e) {
            log.error("CRITICAL: Failed to rollback debit for account {} - Manual intervention required!", accountId, e);
            // In production, this should trigger alerts for manual reconciliation
        }
    }

    /**
     * Get account details by account number.
     * Returns Map with account info if found, or null if not found (404).
     */
    public java.util.Map<String, Object> getAccountByNumber(String accountNumber) {
        String url = accountServiceUrl + "/accounts/by-number/" + accountNumber;
        log.info("Resolving account number: {}", accountNumber);

        try {
            ResponseEntity<java.util.Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    java.util.Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Account number not found: {}", accountNumber);
            return null;
        } catch (Exception e) {
            log.error("Failed to resolve account number {}: {}", accountNumber, e.getMessage());
            throw new AccountServiceException("Failed to resolve account number", e);
        }
        return null;
    }

    
    /**
     * Get userId by accountId.
     * Safely returns null if account is not found or external, ensuring transaction flow continues.
     */
    public String getUserIdByAccountId(String accountId) {
        if (accountId == null) return null;
        
        String url = accountServiceUrl + "/accounts/" + accountId;
        log.debug("Resolving userId for account: {}", accountId);

        try {
            ResponseEntity<java.util.Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    java.util.Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object userIdObj = response.getBody().get("userId");
                if (userIdObj != null) {
                    return userIdObj.toString();
                }
            }
        } catch (HttpClientErrorException.NotFound e) {
            // Normal for external accounts
            log.warn("Account not found in local DB (likely external): {}", accountId);
        } catch (Exception e) {
            // Suppress other errors to avoid breaking the flow
            log.warn("Failed to resolve userId for account {}: {}", accountId, e.getMessage());
        }
        
        return null;
    }

    /**
     * Execute internal transfer atomically (RECOMMENDED).
     * Both debit and credit happen in a single database transaction.
     * Either both succeed or both fail - no partial state.
     */
    public InternalTransferResponse executeInternalTransfer(
            String fromAccountId,
            String toAccountId,
            BigDecimal amount,
            String transactionId,
            String description) {
        
        String url = accountServiceUrl + "/accounts/internal-transfer";
        
        InternalTransferRequest request = InternalTransferRequest.builder()
                .transactionId(transactionId)
                .senderAccountId(fromAccountId)
                .receiverAccountId(toAccountId)
                .amount(amount)
                .description(description)
                .build();

        log.info("Executing internal transfer - From: {} To: {} Amount: {} TxID: {}", 
                fromAccountId, toAccountId, amount, transactionId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<InternalTransferRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<InternalTransferResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    InternalTransferResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Internal transfer completed - TxID: {} - Sender new balance: {} - Receiver new balance: {}", 
                        transactionId, 
                        response.getBody().getSenderAccountNewBalance(),
                        response.getBody().getReceiverAccountNewBalance());
                return response.getBody();
            } else {
                log.error("Unexpected response from account service: {}", response.getStatusCode());
                throw new AccountServiceException("Unexpected response from account service");
            }

        } catch (HttpClientErrorException e) {
            log.error("Client error during internal transfer: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST && e.getResponseBodyAsString().contains("Insufficient balance")) {
                throw new InsufficientBalanceException("Insufficient balance in sender account");
            } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new AccountServiceException("One or both accounts not found");
            }
            
            throw new AccountServiceException("Failed to execute internal transfer: " + e.getMessage(), e);

        } catch (HttpServerErrorException e) {
            log.error("Server error from account service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AccountServiceException("Account service is temporarily unavailable", e);

        } catch (ResourceAccessException e) {
            log.error("Timeout or connection error while calling account service", e);
            throw new AccountServiceException("Cannot connect to account service - timeout", e);

        } catch (Exception e) {
            log.error("Unexpected error during internal transfer", e);
            throw new AccountServiceException("Unexpected error: " + e.getMessage(), e);
        }
    }
}
