package com.uit.referenceservice.mapper;

import com.uit.referenceservice.dto.response.BranchResponse;
import com.uit.referenceservice.entity.Branch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BranchMapper Unit Tests")
class BranchMapperTest {

    private final BranchMapper mapper = Mappers.getMapper(BranchMapper.class);

    @Test
    @DisplayName("toDto() maps Branch to BranchResponse")
    void testToDto() {
        Branch branch = new Branch();
        branch.setBranchId(1);
        branch.setBranchName("Main Branch");
        branch.setBankCode("VCB");

        BranchResponse dto = mapper.toDto(branch);

        assertThat(dto).isNotNull();
        assertThat(dto.getBranchId()).isEqualTo(branch.getBranchId());
        assertThat(dto.getBranchName()).isEqualTo(branch.getBranchName());
        assertThat(dto.getBankCode()).isEqualTo(branch.getBankCode());
    }

    @Test
    @DisplayName("toDtoList() maps list of Branches")
    void testToDtoList() {
        Branch b1 = new Branch();
        b1.setBranchId(1);
        Branch b2 = new Branch();
        b2.setBranchId(2);

        List<BranchResponse> dtos = mapper.toDtoList(Arrays.asList(b1, b2));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getBranchId()).isEqualTo(1);
    }
}

