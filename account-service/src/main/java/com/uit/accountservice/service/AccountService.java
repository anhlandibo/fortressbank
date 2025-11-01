package com.uit.accountservice.service;

import com.uit.accountservice.dto.AccountDto;
import com.uit.accountservice.dto.PendingTransfer;
import com.uit.accountservice.dto.request.SendSmsOtpRequest;
import com.uit.accountservice.dto.request.TransferRequest;
import com.uit.accountservice.dto.request.VerifyTransferRequest;
import com.uit.accountservice.dto.response.ChallengeResponse;
import com.uit.accountservice.entity.Account;
import com.uit.accountservice.entity.TransferStatus;
import com.uit.accountservice.mapper.AccountMapper;
import com.uit.accountservice.repository.AccountRepository;
import com.uit.accountservice.riskengine.RiskEngineService;
import com.uit.accountservice.riskengine.dto.RiskAssessmentRequest;
import com.uit.accountservice.riskengine.dto.RiskAssessmentResponse;
import com.uit.sharedkernel.exception.AppException;
import com.uit.sharedkernel.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j  
@Service
@RequiredArgsConstructor
public class AccountService {

    // Hardcoded phone number for SMS OTPs - FOR DEVELOPMENT ONLY
    public static final String HARDCODED_PHONE_NUMBER = "+84382505668";

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final RiskEngineService riskEngineService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WebClient.Builder webClientBuilder;
    private final TransferAuditService auditService;

    public List<AccountDto> getAccountsByUserId(String userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Validates if the given user owns the specified account.
     * Used for authorization checks before allowing account operations.
     * 
     * @param accountId the account ID to check
     * @param username the username (subject from JWT)
     * @return true if the user owns the account, false otherwise
     */
    public boolean isOwner(String accountId, String username) {
        if (accountId == null || username == null) {
            return false;
        }
        
        return accountRepository.findById(accountId)
                .map(account -> account.getUserId().equals(username))
                .orElse(false);
    }

    @Transactional
    public Object handleTransfer(TransferRequest transferRequest, String userId, 
                                 String deviceFingerprint, String ipAddress, String location) {
        // Security checks
        Account sourceAccount = accountRepository.findById(transferRequest.getFromAccountId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Source account not found"));

        if (!sourceAccount.getUserId().equals(userId)) {
            // Audit: Unauthorized access attempt
            auditService.logTransfer(
                    userId,
                    transferRequest.getFromAccountId(),
                    transferRequest.getToAccountId(),
                    transferRequest.getAmount(),
                    TransferStatus.FAILED,
                    null,
                    null,
                    deviceFingerprint,
                    ipAddress,
                    location,
                    "Unauthorized: User does not own source account"
            );
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (sourceAccount.getBalance().compareTo(transferRequest.getAmount()) < 0) {
            // Audit: Insufficient funds
            auditService.logTransfer(
                    userId,
                    transferRequest.getFromAccountId(),
                    transferRequest.getToAccountId(),
                    transferRequest.getAmount(),
                    TransferStatus.FAILED,
                    null,
                    null,
                    deviceFingerprint,
                    ipAddress,
                    location,
                    "Insufficient funds"
            );
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        // Risk assessment with enhanced fraud detection
        RiskAssessmentResponse riskAssessment;
        try {
            riskAssessment = riskEngineService.assessRisk(
                    RiskAssessmentRequest.builder()
                            .amount(transferRequest.getAmount())
                            .userId(userId)
                            .payeeId(transferRequest.getToAccountId())
                            .deviceFingerprint(deviceFingerprint)
                            .ipAddress(ipAddress)
                            .location(location)
                            .build());
        } catch (Exception e) {
            // Audit: Risk assessment failed
            auditService.logTransfer(
                    userId,
                    transferRequest.getFromAccountId(),
                    transferRequest.getToAccountId(),
                    transferRequest.getAmount(),
                    TransferStatus.FAILED,
                    null,
                    null,
                    deviceFingerprint,
                    ipAddress,
                    location,
                    "Risk assessment service unavailable"
            );
            throw new AppException(ErrorCode.RISK_ASSESSMENT_FAILED);
        }

        if (!riskAssessment.getRiskLevel().equals("LOW")) {
            // Step-up authentication required
            String challengeId = UUID.randomUUID().toString();
            String otpCode = String.valueOf(100000 + (int) (Math.random() * 900000));

            // Send OTP via notification-service
            try {
                webClientBuilder.build()
                    .post()
                    .uri("http://notification-service:4002/notifications/sms/send-otp")
                    .bodyValue(new SendSmsOtpRequest(HARDCODED_PHONE_NUMBER, otpCode))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();  
            } catch (Exception e) {
                log.error("Failed to send OTP", e);
                // Audit: OTP sending failed
                auditService.logTransfer(
                        userId,
                        transferRequest.getFromAccountId(),
                        transferRequest.getToAccountId(),
                        transferRequest.getAmount(),
                        TransferStatus.FAILED,
                        riskAssessment.getRiskLevel(),
                        riskAssessment.getChallengeType(),
                        deviceFingerprint,
                        ipAddress,
                        location,
                        "Failed to send OTP"
                );
                throw new AppException(ErrorCode.NOTIFICATION_SERVICE_FAILED);
            }

            // Store pending transfer in Redis
            PendingTransfer pendingTransfer = new PendingTransfer(
                    transferRequest, 
                    otpCode, 
                    userId, 
                    deviceFingerprint, 
                    ipAddress, 
                    location,
                    riskAssessment.getRiskLevel(),
                    riskAssessment.getChallengeType()
            );
            redisTemplate.opsForValue().set("transfer:" + challengeId, pendingTransfer, 5, TimeUnit.MINUTES);

            // Audit: Challenge issued (pending OTP verification)
            auditService.logTransfer(
                    userId,
                    transferRequest.getFromAccountId(),
                    transferRequest.getToAccountId(),
                    transferRequest.getAmount(),
                    TransferStatus.PENDING,
                    riskAssessment.getRiskLevel(),
                    riskAssessment.getChallengeType(),
                    deviceFingerprint,
                    ipAddress,
                    location,
                    "OTP challenge issued"
            );

            return new ChallengeResponse("CHALLENGE_REQUIRED", challengeId, riskAssessment.getChallengeType());
        } else {
            // Execute transfer immediately (low risk)
            AccountDto result = createTransfer(transferRequest, userId, deviceFingerprint, ipAddress, location, riskAssessment);
            return result;
        }
    }

    @Transactional
    public AccountDto verifyTransfer(VerifyTransferRequest verifyTransferRequest) {
        // Retrieve pending transfer from Redis
        PendingTransfer pendingTransfer;
        try {
            pendingTransfer = (PendingTransfer) redisTemplate.opsForValue().get("transfer:" + verifyTransferRequest.getChallengeId());
        } catch (Exception e) {
            throw new AppException(ErrorCode.REDIS_CONNECTION_FAILED);
        }

        if (pendingTransfer == null) {
            // Audit: Challenge not found or expired
            auditService.logTransfer(
                    null,  // userId not available when challenge not found
                    null,
                    null,
                    null,
                    TransferStatus.EXPIRED,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Challenge ID not found or expired"
            );
            throw new AppException(ErrorCode.NOT_FOUND_EXCEPTION, "Pending transfer not found");
        }

        if (!pendingTransfer.getOtpCode().equals(verifyTransferRequest.getOtpCode())) {
            // Audit: Invalid OTP attempt
            auditService.logTransfer(
                    pendingTransfer.getUserId(),
                    pendingTransfer.getTransferRequest().getFromAccountId(),
                    pendingTransfer.getTransferRequest().getToAccountId(),
                    pendingTransfer.getTransferRequest().getAmount(),
                    TransferStatus.FAILED,
                    pendingTransfer.getRiskLevel(),
                    pendingTransfer.getChallengeType(),
                    pendingTransfer.getDeviceFingerprint(),
                    pendingTransfer.getIpAddress(),
                    pendingTransfer.getLocation(),
                    "Invalid OTP code"
            );
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        // Execute the transfer
        AccountDto accountDto = createTransfer(
                pendingTransfer.getTransferRequest(), 
                pendingTransfer.getUserId(),
                pendingTransfer.getDeviceFingerprint(),
                pendingTransfer.getIpAddress(),
                pendingTransfer.getLocation(),
                null  // RiskAssessment already done, pass null
        );

        // Delete the used challenge from Redis
        redisTemplate.delete("transfer:" + verifyTransferRequest.getChallengeId());

        return accountDto;
    }

    private AccountDto createTransfer(TransferRequest transferRequest, String userId,
                                      String deviceFingerprint, String ipAddress, String location,
                                      RiskAssessmentResponse riskAssessment) {
        try {
            Account fromAccount = accountRepository.findById(transferRequest.getFromAccountId())
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Source account not found"));
            Account toAccount = accountRepository.findById(transferRequest.getToAccountId())
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Destination account not found"));

            fromAccount.setBalance(fromAccount.getBalance().subtract(transferRequest.getAmount()));
            toAccount.setBalance(toAccount.getBalance().add(transferRequest.getAmount()));

            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            // Audit: Successful transfer (immediate low-risk or after OTP verification)
            String riskLevel = riskAssessment != null ? riskAssessment.getRiskLevel() : "LOW";
            String challengeType = riskAssessment != null ? riskAssessment.getChallengeType() : null;
            
            auditService.logTransfer(
                    userId,
                    transferRequest.getFromAccountId(),
                    transferRequest.getToAccountId(),
                    transferRequest.getAmount(),
                    TransferStatus.COMPLETED,
                    riskLevel,
                    challengeType,
                    deviceFingerprint,
                    ipAddress,
                    location,
                    "Transfer executed successfully"
            );

            return accountMapper.toDto(fromAccount);
        } catch (Exception e) {
            // Audit: Transfer execution failed
            String riskLevel = riskAssessment != null ? riskAssessment.getRiskLevel() : null;
            String challengeType = riskAssessment != null ? riskAssessment.getChallengeType() : null;
            
            auditService.logTransfer(
                    userId,
                    transferRequest.getFromAccountId(),
                    transferRequest.getToAccountId(),
                    transferRequest.getAmount(),
                    TransferStatus.FAILED,
                    riskLevel,
                    challengeType,
                    deviceFingerprint,
                    ipAddress,
                    location,
                    "Transfer execution error: " + e.getMessage()
            );
            throw e;
        }
    }
}