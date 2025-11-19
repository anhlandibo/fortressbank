package com.uit.referenceservice.mapper;

import com.uit.referenceservice.dto.response.BranchResponse;
import com.uit.referenceservice.entity.Branch;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BranchMapper {
    BranchResponse toDto(Branch branch);
    List<BranchResponse> toDtoList(List<Branch> branches);
}

