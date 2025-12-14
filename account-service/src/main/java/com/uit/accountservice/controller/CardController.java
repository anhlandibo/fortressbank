package com.uit.accountservice.controller;

import com.uit.accountservice.dto.CardDto;
import com.uit.accountservice.service.CardService;
import com.uit.sharedkernel.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardService cardService;

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // Lấy danh sách thẻ của 1 account
    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<List<CardDto>>> getCards(@PathVariable("accountId") String accountId) {
        return ResponseEntity.ok(ApiResponse.success(cardService.getCardsByAccountId(accountId, getCurrentUserId())));
    }

    // Phát hành thẻ mới VIRTUAL (Card type is always VIRTUAL)
    @PostMapping("/account/{accountId}/issue")
    public ResponseEntity<ApiResponse<CardDto>> issueCard(@PathVariable("accountId") String accountId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cardService.issueCard(getCurrentUserId(), accountId)));
    }

    // Khóa/Mở thẻ
    @PostMapping("/{cardId}/toggle-lock")
    public ResponseEntity<ApiResponse<Void>> toggleLock(@PathVariable("cardId") String cardId) {
        cardService.toggleCardLock(cardId, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
