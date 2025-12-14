package com.uit.accountservice.service;

import com.uit.accountservice.client.UserClient;
import com.uit.accountservice.dto.CardDto;
import com.uit.accountservice.dto.response.UserResponse;
import com.uit.accountservice.entity.Account;
import com.uit.accountservice.entity.Card;
import com.uit.accountservice.entity.enums.CardStatus;
import com.uit.accountservice.entity.enums.CardType;
import com.uit.accountservice.repository.AccountRepository;
import com.uit.accountservice.repository.CardRepository;
import com.uit.sharedkernel.api.ApiResponse;
import com.uit.sharedkernel.exception.AppException;
import com.uit.sharedkernel.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserClient userClient;

    public List<CardDto> getCardsByAccountId(String accountId, String userId) {
        validateAccountOwnership(accountId, userId);

        return cardRepository.findByAccountId(accountId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CardDto issueCard(String userId, String accountId){
        Account account = validateAccountOwnership(accountId, userId);

        String cardHolderName = "UNKNOWN";
        try {
            ApiResponse<UserResponse> response = userClient.getUserById(userId);
            if (response != null && response.getData() != null && response.getData().fullName() != null) {
                cardHolderName = response.getData().fullName().toUpperCase();
            } else {
                log.warn("User fullName is null for userId: {}. Using default cardHolderName.", userId);
            }
        } catch (Exception e) {
            log.error("Failed to fetch user info for card issuance for userId: {}. Error: {}", userId, e.getMessage());
        }
        String cardNumber = generateLuhnCardNumber();
        String cvv = generateRandomDigits(3);

        log.info("ISSUING CARD - User: {}, AccountId: {}, CVV: {}", userId, accountId, cvv);
        LocalDate expiryDate = LocalDate.now().plusYears(5); // Hết hạn sau 5 năm

        Card card = Card.builder()
                .accountId(account.getAccountId())
                .cardNumber(cardNumber)
                .cardHolderName(cardHolderName)
                .cvvHash(passwordEncoder.encode(cvv)) 
                .expirationDate(expiryDate)
                .cardType(CardType.VIRTUAL) 
                .status(CardStatus.ACTIVE)
                .build();

        cardRepository.save(card);

        log.info("VIRTUAL card issued successfully - CardNumber: {} (masked)", maskCardNumber(cardNumber));

        return toDto(card);
    }

    public void createInitialCard(Account account, String fullName) {
        String cardHolderName = (fullName != null && !fullName.isEmpty()) ? fullName.toUpperCase() : "VALUED CUSTOMER";
        
        String cardNumber = generateLuhnCardNumber();
        String cvv = generateRandomDigits(3);
        LocalDate expiryDate = LocalDate.now().plusYears(5);

        Card card = Card.builder()
                .accountId(account.getAccountId())
                .cardNumber(cardNumber)
                .cardHolderName(cardHolderName)
                .cvvHash(passwordEncoder.encode(cvv))
                .expirationDate(expiryDate)
                .cardType(CardType.VIRTUAL) 
                .status(CardStatus.ACTIVE)
                .build();

        cardRepository.save(card);
        
        // Log thông tin quan trọng (Masked)
        log.info("AUTO-CREATED: VIRTUAL card issued for Account {} - User: {} - Card: {}", 
                account.getAccountId(), cardHolderName, maskCardNumber(cardNumber));
    }

    // --- KHÓA/MỞ THẺ ---
    @Transactional
    public void toggleCardLock(String cardId, String userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_EXCEPTION, "Card not found"));

        validateAccountOwnership(card.getAccountId(), userId);

        if (card.getStatus() == CardStatus.ACTIVE) {
            card.setStatus(CardStatus.LOCKED);
        } else if (card.getStatus() == CardStatus.LOCKED) {
            card.setStatus(CardStatus.ACTIVE);
        }
        cardRepository.save(card);
    }

    // --- HELPERS ---
    private Account validateAccountOwnership(String accountId, String userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (!account.getUserId().equals(userId))
            throw new AppException(ErrorCode.FORBIDDEN);

        return account;
    }

    private CardDto toDto(Card card) {
        return CardDto.builder()
                .cardId(card.getCardId())
                .cardNumber(maskCardNumber(card.getCardNumber())) // **** 1234
                .cardHolderName(card.getCardHolderName())
                .expirationDate(card.getExpirationDate().toString())
                .status(card.getStatus().name())
                .cardType(card.getCardType().name())
                .build();
    }

    private String maskCardNumber(String fullNumber) {
        // Giấu 12 số đầu, chỉ hiện 4 số cuối
        if (fullNumber == null || fullNumber.length() < 4) return fullNumber;
        return "**** **** **** " + fullNumber.substring(fullNumber.length() - 4);
    }

    private String generateLuhnCardNumber() {
        String bin = "6886";
        StringBuilder builder = new StringBuilder(bin);

        // Sinh 11 số ngẫu nhiên tiếp theo
        for (int i = 0; i < 11; i++) {
            builder.append((int) (Math.random() * 10));
        }

        // Tính số Checksum cuối cùng (Luhn Digit) - Ở đây mình làm đơn giản là random nốt số cuối
        builder.append((int) (Math.random() * 10));

        return builder.toString();
    }

    private String generateRandomDigits(int length) {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<length; i++) sb.append((int)(Math.random() * 10));
        return sb.toString();
    }

    private String getAccountHolderName(String userId) {
        // Tạm thời trả về tên cố định, sau này có thể gọi User Service để lấy tên thật
        return "NGO MINH TRI";
    }

    public CardDto issueCardInternal(String accountId, String fullName) {
        // 1. Tìm Account để lấy UserId (Tin tưởng User Service đã tạo account thành công)
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
                
        String cardHolderName = (fullName != null) ? fullName.toUpperCase() : "UNKNOWN";
        String cardNumber = generateLuhnCardNumber();
        String cvv = generateRandomDigits(3);
        LocalDate expiryDate = LocalDate.now().plusYears(5);

        Card card = Card.builder()
                .accountId(account.getAccountId())
                .cardNumber(cardNumber)
                .cardHolderName(cardHolderName)
                .cvvHash(passwordEncoder.encode(cvv))
                .expirationDate(expiryDate)
                .cardType(CardType.VIRTUAL)
                .status(CardStatus.ACTIVE)
                .build();

        cardRepository.save(card);
        log.info("INTERNAL: VIRTUAL card issued for Account {} - User {}", accountId, cardHolderName);

        return toDto(card);
    }
}
