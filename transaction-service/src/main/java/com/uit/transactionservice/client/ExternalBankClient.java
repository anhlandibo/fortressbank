package com.uit.transactionservice.client;

import com.uit.transactionservice.client.dto.ExternalTransferRequest;
import com.uit.transactionservice.client.dto.ExternalTransferResponse;
import com.uit.transactionservice.exception.ExternalBankException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for async communication with external banks.
 * Used for interbank transfers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalBankClient {

    private final RestTemplate restTemplate;

    @Value("${services.external-bank.url:http://external-bank-mock:5000}")
    private String externalBankUrl;

    /**
     * Initiate external bank transfer (async operation).
     * The external bank will process asynchronously and send callback.
     * 
     * @return External transaction reference ID for tracking
     */
    public ExternalTransferResponse initiateTransfer(ExternalTransferRequest request) {
        String url = externalBankUrl + "/api/external-bank/transfer";

        log.info("Initiating external transfer - TxID: {} To Bank: {} Account: {} Amount: {}",
                request.getTransactionId(),
                request.getDestinationBankCode(),
                request.getDestinationAccountNumber(),
                request.getAmount());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ExternalTransferRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ExternalTransferResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ExternalTransferResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("External transfer initiated - TxID: {} - External Ref: {} - Status: {}",
                        request.getTransactionId(),
                        response.getBody().getExternalTransactionId(),
                        response.getBody().getStatus());
                return response.getBody();
            } else {
                log.error("Unexpected response from external bank: {}", response.getStatusCode());
                throw new ExternalBankException("Unexpected response from external bank");
            }

        } catch (HttpClientErrorException e) {
            log.error("Client error during external transfer: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalBankException("Failed to initiate external transfer: " + e.getMessage(), e);

        } catch (HttpServerErrorException e) {
            log.error("Server error from external bank: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalBankException("External bank service is temporarily unavailable", e);

        } catch (ResourceAccessException e) {
            log.error("Timeout or connection error while calling external bank", e);
            throw new ExternalBankException("Cannot connect to external bank - timeout", e);

        } catch (Exception e) {
            log.error("Unexpected error during external transfer initiation", e);
            throw new ExternalBankException("Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Query status of external transfer.
     * Used to check if transfer completed, failed, or still pending.
     */
    public ExternalTransferResponse queryTransferStatus(String externalTransactionId) {
        String url = externalBankUrl + "/api/external-bank/transfer/status/" + externalTransactionId;

        log.info("Querying external transfer status - External Ref: {}", externalTransactionId);

        try {
            ResponseEntity<ExternalTransferResponse> response = restTemplate.getForEntity(
                    url,
                    ExternalTransferResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("External transfer status - Ref: {} - Status: {}",
                        externalTransactionId,
                        response.getBody().getStatus());
                return response.getBody();
            } else {
                log.error("Unexpected response while querying external bank: {}", response.getStatusCode());
                throw new ExternalBankException("Unexpected response from external bank");
            }

        } catch (Exception e) {
            log.error("Error querying external transfer status", e);
            throw new ExternalBankException("Failed to query transfer status: " + e.getMessage(), e);
        }
    }
}
