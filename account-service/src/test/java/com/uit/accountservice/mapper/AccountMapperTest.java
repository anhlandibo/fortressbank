package com.uit.accountservice.mapper;

import com.uit.accountservice.dto.AccountDto;
import com.uit.accountservice.entity.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccountMapper Unit Tests")
class AccountMapperTest {

    private final AccountMapper mapper = Mappers.getMapper(AccountMapper.class);

    @Test
    @DisplayName("toDto() maps Account to AccountDto")
    void testToDto() {
        Account account = Account.builder()
                .accountId("acc-123")
                .userId("user-123")
                .balance(BigDecimal.valueOf(1000.00))
                .createdAt(LocalDateTime.now())
                .build();

        AccountDto dto = mapper.toDto(account);

        assertThat(dto).isNotNull();
        assertThat(dto.getAccountId()).isEqualTo(account.getAccountId());
        assertThat(dto.getUserId()).isEqualTo(account.getUserId());
        assertThat(dto.getBalance()).isEqualTo(account.getBalance());
    }

    @Test
    @DisplayName("toDto() handles null input")
    void testToDto_NullInput() {
        AccountDto dto = mapper.toDto(null);
        assertThat(dto).isNull();
    }
}

