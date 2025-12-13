package com.uit.accountservice.service;

import com.uit.accountservice.client.UserClient;
import com.uit.accountservice.dto.CardDto;
import com.uit.accountservice.dto.request.IssueCardRequest;
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

    // LẤY DANH SÁCH THẺ
    public List<CardDto> getCardsByAccountId(String accountId, String userId) {
        validateAccountOwnership(accountId, userId);

        return cardRepository.findByAccountId(accountId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // PHÁT HÀNH THẺ MỚI
    public CardDto issueCard(String userId, IssueCardRequest request){
        Account account = validateAccountOwnership(request.accountId(), userId);

        String cardHolderName = "UNKNOWN";
        try {
            ApiResponse<UserResponse> response = userClient.getUserById(userId);
            if (response != null && response.getData() != null) {
                // Tên trên thẻ thường viết hoa không dấu
                cardHolderName = response.getData().fullName().toUpperCase();
            }
        } catch (Exception e) {
            // Fallback: Nếu gọi user-service lỗi, có thể lấy từ Token hoặc để default
            log.error("Failed to fetch user info for card issuance", e);
        }
        String cardNumber = generateLuhnCardNumber();
        String cvv = generateRandomDigits(3);

        log.info("ISSUING CARD - User: {}, CVV: {}", userId, cvv);
        LocalDate expiryDate = LocalDate.now().plusYears(5); // Hết hạn sau 5 năm

        Card card = Card.builder()
                .accountId(account.getAccountId())
                .cardNumber(cardNumber)
                .cardHolderName(cardHolderName) 
                .cvvHash(passwordEncoder.encode(cvv)) // Hash CVV
                .expirationDate(expiryDate)
                .cardType(CardType.valueOf(request.cardType().toUpperCase()))
                .status(CardStatus.ACTIVE)
                .build();

        cardRepository.save(card);

        return toDto(card);
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
}
