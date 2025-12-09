package com.uit.accountservice.mapper;

import com.uit.accountservice.dto.AccountDto;
import com.uit.accountservice.entity.Account;
import com.uit.accountservice.entity.enums.AccountStatus;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface AccountMapper {
    @Mapping(target = "accountStatus", source = "status")
    AccountDto toDto(Account account);

    default String map(AccountStatus status) {
        return status != null ? status.name() : null;
    }
}
