package com.uit.accountservice.service;

import com.uit.accountservice.dto.AccountDto;
import com.uit.accountservice.dto.request.TransferRequest;
import com.uit.accountservice.dto.request.VerifyTransferRequest;
import com.uit.accountservice.dto.response.ChallengeResponse;
import com.uit.accountservice.entity.Account;
import com.uit.accountservice.mapper.AccountMapper;
import com.uit.accountservice.repository.AccountRepository;
import com.uit.accountservice.riskengine.RiskEngineService;
import com.uit.accountservice.riskengine.dto.RiskAssessmentRequest;
import com.uit.accountservice.riskengine.dto.RiskAssessmentResponse;
import com.uit.sharedkernel.exception.AppException;
import com.uit.sharedkernel.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final RiskEngineService riskEngineService;
    private final RedisTemplate<String, Object> redisTemplate;

    public List<AccountDto> getAccountsByUserId(String userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Object handleTransfer(TransferRequest transferRequest, String userId) {
        // Security checks
        Account sourceAccount = accountRepository.findById(transferRequest.getFromAccountId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_EXCEPTION));

        if (!sourceAccount.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "You can only transfer from your own accounts.");
        }

        if (sourceAccount.getBalance().compareTo(transferRequest.getAmount()) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        // Risk assessment
        RiskAssessmentResponse riskAssessment = riskEngineService.assessRisk(
                new RiskAssessmentRequest(transferRequest.getAmount(), userId, transferRequest.getToAccountId()));

        if (!riskAssessment.getRiskLevel().equals("LOW")) {
            // Step-up authentication
            String challengeId = UUID.randomUUID().toString();
            String otpCode = String.valueOf(100000 + (int) (Math.random() * 900000));

            // TODO: Send real SMS
            TransferRequest pendingTransfer = new TransferRequest();
            pendingTransfer.setFromAccountId(transferRequest.getFromAccountId());
            pendingTransfer.setToAccountId(transferRequest.getToAccountId());
            pendingTransfer.setAmount(transferRequest.getAmount());
            // Store pending transfer in Redis
            redisTemplate.opsForValue().set("transfer:" + challengeId, pendingTransfer, 5, TimeUnit.MINUTES);

            return new ChallengeResponse("CHALLENGE_REQUIRED", challengeId, riskAssessment.getChallengeType());
        } else {
            // Execute transfer immediately
            return createTransfer(transferRequest);
        }
    }

    @Transactional
    public AccountDto verifyTransfer(VerifyTransferRequest verifyTransferRequest) {
        // Retrieve pending transfer from Redis
        TransferRequest pendingTransfer = (TransferRequest) redisTemplate.opsForValue().get("transfer:" + verifyTransferRequest.getChallengeId());

        if (pendingTransfer == null) {
            throw new AppException(ErrorCode.NOT_FOUND_EXCEPTION, "Challenge not found or has expired.");
        }

        // TODO: Implement OTP check

        // Execute the transfer
        AccountDto accountDto = createTransfer(pendingTransfer);

        // Delete the used challenge from Redis
        redisTemplate.delete("transfer:" + verifyTransferRequest.getChallengeId());

        return accountDto;
    }

    private AccountDto createTransfer(TransferRequest transferRequest) {
        Account fromAccount = accountRepository.findById(transferRequest.getFromAccountId()).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_EXCEPTION));
        Account toAccount = accountRepository.findById(transferRequest.getToAccountId()).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_EXCEPTION));

        fromAccount.setBalance(fromAccount.getBalance().subtract(transferRequest.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(transferRequest.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        return accountMapper.toDto(fromAccount);
    }
}