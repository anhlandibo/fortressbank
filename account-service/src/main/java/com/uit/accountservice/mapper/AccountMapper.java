package com.uit.accountservice.mapper;

import com.uit.accountservice.dto.AccountDto;
import com.uit.accountservice.entity.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountDto toDto(Account account);
}
