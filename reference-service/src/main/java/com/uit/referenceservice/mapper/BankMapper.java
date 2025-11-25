package com.uit.referenceservice.mapper;

import com.uit.referenceservice.dto.response.BankResponse;
import com.uit.referenceservice.entity.Bank;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BankMapper {
    BankResponse toDto(Bank bank);
    List<BankResponse> toDtoList(List<Bank> banks);
}

