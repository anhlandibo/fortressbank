package com.uit.accountservice.service;

import com.uit.accountservice.dto.AccountDto;
import com.uit.accountservice.dto.PendingTransfer;
import com.uit.accountservice.dto.request.CreateAccountRequest;
import com.uit.accountservice.dto.request.SendSmsOtpRequest;
import com.uit.accountservice.dto.request.TransferRequest;
import com.uit.accountservice.dto.request.VerifyTransferRequest;
import com.uit.accountservice.dto.response.ChallengeResponse;
import com.uit.accountservice.entity.Account;
import com.uit.accountservice.entity.enums.AccountStatus;
import com.uit.accountservice.entity.enums.TransferStatus;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j  
@Service
@RequiredArgsConstructor
public class AccountService {

    // Hardcoded phone number for SMS OTPs - FOR DEVELOPMENT ONLY
    public static final String HARDCODED_PHONE_NUMBER = "+84857311444";

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final RiskEngineService riskEngineService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WebClient.Builder webClientBuilder;
    private final TransferAuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public List<AccountDto> getAccountsByUserId(String userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<AccountDto> getAllAccounts() {
        return accountRepository.findAll()
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
        Account sourceAccount = accountRepository.findById(transferRequest.getSenderAccountId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Source account not found"));

        if (!sourceAccount.getUserId().equals(userId)) {
            // Audit: Unauthorized access attempt
            auditService.logTransfer(
                    userId,
                    transferRequest.getSenderAccountId(),
                    transferRequest.getReceiverAccountId(),
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
                    transferRequest.getSenderAccountId(),
                    transferRequest.getReceiverAccountId(),
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
                            .payeeId(transferRequest.getReceiverAccountId())
                            .deviceFingerprint(deviceFingerprint)
                            .ipAddress(ipAddress)
                            .location(location)
                            .build());
        } catch (Exception e) {
            // Audit: Risk assessment failed
            auditService.logTransfer(
                    userId,
                    transferRequest.getSenderAccountId(),
                    transferRequest.getReceiverAccountId(),
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
                        transferRequest.getSenderAccountId(),
                        transferRequest.getReceiverAccountId(),
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
                    transferRequest.getSenderAccountId(),
                    transferRequest.getReceiverAccountId(),
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
                    pendingTransfer.getTransferRequest().getSenderAccountId(),
                    pendingTransfer.getTransferRequest().getReceiverAccountId(),
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
            Account fromAccount = accountRepository.findById(transferRequest.getSenderAccountId())
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Source account not found"));
            Account toAccount = accountRepository.findById(transferRequest.getReceiverAccountId())
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
                    transferRequest.getSenderAccountId(),
                    transferRequest.getReceiverAccountId(),
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
                    transferRequest.getSenderAccountId(),
                    transferRequest.getReceiverAccountId(),
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

    /**
     * Debit (subtract) amount from an account.
     * Called by transaction-service for synchronous balance updates.
     * Uses pessimistic locking to prevent race conditions.
     */
    @Transactional
    public com.uit.accountservice.dto.response.AccountBalanceResponse debitAccount(
            String accountId, 
            com.uit.accountservice.dto.request.AccountBalanceRequest request) {
        
        log.info("Debiting account {} - Amount: {} - Transaction: {}", 
                accountId, request.getAmount(), request.getTransactionId());

        // Find account WITH PESSIMISTIC LOCK to prevent concurrent modifications
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, 
                        "Account not found: " + accountId));

        BigDecimal oldBalance = account.getBalance();

        // Check sufficient balance (double-check after acquiring lock)
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Insufficient balance for account {} - Required: {} - Available: {}", 
                    accountId, request.getAmount(), account.getBalance());
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS, 
                    "Insufficient balance in account: " + accountId);
        }

        // Deduct amount atomically
        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        log.info("Debit successful - Account: {} - Old balance: {} - New balance: {} - TxID: {}", 
                accountId, oldBalance, newBalance, request.getTransactionId());

        return com.uit.accountservice.dto.response.AccountBalanceResponse.builder()
                .accountId(accountId)
                .oldBalance(oldBalance)
                .newBalance(newBalance)
                .transactionId(request.getTransactionId())
                .success(true)
                .message("Debit successful")
                .build();
    }

    /**
     * Credit (add) amount to an account.
     * Called by transaction-service for synchronous balance updates.
     * Uses pessimistic locking to prevent race conditions.
     */
    @Transactional
    public com.uit.accountservice.dto.response.AccountBalanceResponse creditAccount(
            String accountId, 
            com.uit.accountservice.dto.request.AccountBalanceRequest request) {
        
        log.info("Crediting account {} - Amount: {} - Transaction: {}", 
                accountId, request.getAmount(), request.getTransactionId());

        // Find account WITH PESSIMISTIC LOCK to prevent concurrent modifications
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, 
                        "Account not found: " + accountId));

        BigDecimal oldBalance = account.getBalance();

        // Add amount atomically
        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        log.info("Credit successful - Account: {} - Old balance: {} - New balance: {} - TxID: {}", 
                accountId, oldBalance, newBalance, request.getTransactionId());

        return com.uit.accountservice.dto.response.AccountBalanceResponse.builder()
                .accountId(accountId)
                .oldBalance(oldBalance)
                .newBalance(newBalance)
                .transactionId(request.getTransactionId())
                .success(true)
                .message("Credit successful")
                .build();
    }

    /**
     * Execute internal transfer atomically in a single transaction.
     * Both debit and credit happen together - either both succeed or both fail.
     * Uses pessimistic locking on BOTH accounts to prevent race conditions.
     */
    @Transactional
    public com.uit.accountservice.dto.response.InternalTransferResponse executeInternalTransfer(
            com.uit.accountservice.dto.request.InternalTransferRequest request) {
        
        log.info("Executing internal transfer - From: {} To: {} Amount: {} TxID: {}", 
                request.getSenderAccountId(), request.getReceiverAccountId(), 
                request.getAmount(), request.getTransactionId());

        // Lock BOTH accounts in deterministic order to prevent deadlock
        List<String> accountIds = List.of(request.getSenderAccountId(), request.getReceiverAccountId());
        List<Account> accounts = accountRepository.findByIdInWithLock(accountIds);
        
        if (accounts.size() != 2) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND, 
                    "One or both accounts not found");
        }
        
        // Identify sender and receiver
        Account fromAccount = accounts.stream()
                .filter(a -> a.getAccountId().equals(request.getSenderAccountId()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, 
                        "Sender account not found"));
        
        Account toAccount = accounts.stream()
                .filter(a -> a.getAccountId().equals(request.getReceiverAccountId()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, 
                        "Receiver account not found"));
        
        // Store old balances for response
        BigDecimal fromOldBalance = fromAccount.getBalance();
        BigDecimal toOldBalance = toAccount.getBalance();
        
        // Check sufficient balance
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Insufficient balance - Account: {} Required: {} Available: {}", 
                    request.getSenderAccountId(), request.getAmount(), fromAccount.getBalance());
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS, 
                    "Insufficient balance in sender account");
        }
        
        // Execute transfer atomically
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        // Save both accounts (within same transaction)
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        log.info("Internal transfer completed - TxID: {} - Sender: {} ({} → {}) - Receiver: {} ({} → {})",
                request.getTransactionId(),
                request.getSenderAccountId(), fromOldBalance, fromAccount.getBalance(),
                request.getReceiverAccountId(), toOldBalance, toAccount.getBalance());

        return com.uit.accountservice.dto.response.InternalTransferResponse.builder()
                .transactionId(request.getTransactionId())
                .senderAccountId(request.getSenderAccountId())
                .senderAccountOldBalance(fromOldBalance)
                .senderAccountNewBalance(fromAccount.getBalance())
                .receiverAccountId(request.getReceiverAccountId())
                .receiverAccountOldBalance(toOldBalance)
                .receiverAccountNewBalance(toAccount.getBalance())
                .amount(request.getAmount())
                .success(true)
                .message("Internal transfer completed successfully")
                .build();
    }

    // SECTION BOLAC <3
    public List<AccountDto> getMyAccounts(String userId) {
        // Chỉ lấy các tài khoản chưa bị đóng
        return accountRepository.findByUserId(userId).stream()
                .filter(a -> a.getStatus() != AccountStatus.CLOSED)
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    public AccountDto getAccountDetail(String accountId, String userId) {
        Account account = getAccountOwnedByUser(accountId, userId);
        return accountMapper.toDto(account);
    }

    public BigDecimal getBalance(String accountId, String userId) {
        Account account = getAccountOwnedByUser(accountId, userId);
        return account.getBalance();
    }

    @Transactional
    public AccountDto createAccount(String userId, CreateAccountRequest request) {
        String accountNumber;
        
        // Determine account number based on accountNumberType
        if ("PHONE_NUMBER".equals(request.accountNumberType())) {
            // Use phone number as account number
            if (request.phoneNumber() == null || request.phoneNumber().isEmpty()) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Phone number is required when accountNumberType is PHONE_NUMBER");
            }
            accountNumber = request.phoneNumber();
            
            // Check if account with this phone number already exists
            if (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Account with this phone number already exists");
            }
        } else if ("AUTO_GENERATE".equals(request.accountNumberType())) {
            // Generate unique account number
            accountNumber = generateUniqueAccountNumber();
        } else {
            throw new AppException(ErrorCode.BAD_REQUEST, "Invalid accountNumberType: " + request.accountNumberType());
        }

        Account account = Account.builder()
                .userId(userId)
                .accountNumber(accountNumber)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        return accountMapper.toDto(accountRepository.save(account));
    }

    @Transactional
    public void closeAccount(String accountId, String userId) {
        Account account = getAccountOwnedByUser(accountId, userId);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new AppException(ErrorCode.ACCOUNT_STATUS_CONFLICT, "Account is already closed");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new AppException(ErrorCode.ACCOUNT_CLOSE_NONZERO_BALANCE);
        }

        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
    }

    @Transactional
    public void createPin(String accountId, String userId, String newPin) {
        validatePinFormat(newPin);
        Account account = getAccountOwnedByUser(accountId, userId);
        if (account.getPinHash() != null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "PIN already exists. Use PUT to update.");
        }
        account.setPinHash(passwordEncoder.encode(newPin));
        accountRepository.save(account);
    }

    @Transactional
    public void updatePin(String accountId, String userId, String oldPin, String newPin) {
        validatePinFormat(newPin);
        Account account = getAccountOwnedByUser(accountId, userId);

        if (account.getPinHash() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "PIN not set. Use POST to create.");
        }

        if (!passwordEncoder.matches(oldPin, account.getPinHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Old PIN is incorrect");
        }

        account.setPinHash(passwordEncoder.encode(newPin));
        accountRepository.save(account);
    }

    /**
     * Create PIN without authentication (for post-registration flow).
     * This allows users to set up PIN immediately after registration without logging in.
     */
    @Transactional
    public void createPinPublic(String accountId, String newPin) {
        validatePinFormat(newPin);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (account.getPinHash() != null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "PIN already exists. Cannot create duplicate PIN.");
        }

        account.setPinHash(passwordEncoder.encode(newPin));
        accountRepository.save(account);
        log.info("PIN created successfully for account {}", accountId);
    }

    /**
     * Verify if the provided PIN matches the account's PIN.
     * Used during transfer flow to validate user authorization.
     */
    public boolean verifyPin(String accountId, String userId, String pin) {
        validatePinFormat(pin);
        Account account = getAccountOwnedByUser(accountId, userId);

        if (account.getPinHash() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "PIN not set for this account");
        }

        return passwordEncoder.matches(pin, account.getPinHash());
    }

    // Helpers
    private Account getAccountOwnedByUser(String accountId, String userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!account.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Access denied");
        }
        return account;
    }

    private String generateUniqueAccountNumber() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        // Số đầu tiên từ 1-9 để tránh số 0 ở đầu
        sb.append(random.nextInt(9) + 1);
        for (int i = 0; i < 9; i++) {
            sb.append(random.nextInt(10));
        }

        String accNum = sb.toString();

        // Đệ quy check trùng (tuy xác suất thấp nhưng cần thiết cho banking)
        if (accountRepository.existsByAccountNumber(accNum)) {
            return generateUniqueAccountNumber();
        }
        return accNum;
    }

    private void validatePinFormat(String pin) {
        if (pin == null || !pin.matches("\\d{6}")) {
            throw new AppException(ErrorCode.BAD_REQUEST, "PIN must be exactly 6 digits");
        }
    }

}