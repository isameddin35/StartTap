package com.bmu1093a.quill.vacancy.model.dto.response;

import com.bmu1093a.quill.startup.model.dto.respone.StartupResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VacancyResponseDto {
    private Long id;

    private String title;

    private String description;

    private BigDecimal salary;


    private StartupResponseDto startup;

    private LocalDateTime createdAt;

    private Boolean isActive;

    private Boolean isOwner;

    private Integer applicationCount;
}
