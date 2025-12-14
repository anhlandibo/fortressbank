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
     * Debit (subtract) amount from an account.
     * Called by transaction-service for synchronous balance updates.
     * Uses pessimistic locking to prevent race conditions.
     */
    public AccountDto getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Account number not found: " + accountNumber));
        return accountMapper.toDto(account);
    }

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
        if (!isValidAccountType(request.accountType())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Invalid account type");
        }
        // Logic sinh số tài khoản ngẫu nhiên 10 số
        String accountNumber = generateUniqueAccountNumber();

        Account account = Account.builder()
                .userId(userId)
                .accountNumber(accountNumber)
                .accountType(request.accountType())
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

    private boolean isValidAccountType(String type) {
        return type != null && (type.equalsIgnoreCase("SPEND") || type.equalsIgnoreCase("SAVING"));
    }

    private void validatePinFormat(String pin) {
        if (pin == null || !pin.matches("\\d{6}")) {
            throw new AppException(ErrorCode.BAD_REQUEST, "PIN must be exactly 6 digits");
        }
    }
}