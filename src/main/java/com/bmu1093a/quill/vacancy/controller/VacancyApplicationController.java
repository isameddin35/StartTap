package com.bmu1093a.quill.vacancy.controller;

import com.bmu1093a.quill.vacancy.model.dto.response.VacancyApplicantResponseDto;
import com.bmu1093a.quill.vacancy.model.dto.response.VacancyApplicationResponseDto;
import com.bmu1093a.quill.vacancy.model.entity.enumeration.ApplicationStatus;
import com.bmu1093a.quill.vacancy.service.VacancyApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/vacancies")
@RequiredArgsConstructor
public class VacancyApplicationController {

    private final VacancyApplicationService vacancyApplicationService;

    @PostMapping("{vacancyId}/applications")
    public ResponseEntity<VacancyApplicationResponseDto> applyToVacancy(
            @PathVariable Long vacancyId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vacancyApplicationService.applyToVacancy(vacancyId));
    }

    @GetMapping("{vacancyId}/applications")
    public ResponseEntity<List<VacancyApplicantResponseDto>> getApplicationsByVacancyId(@PathVariable Long vacancyId) {
        return ResponseEntity.status(HttpStatus.OK).body(vacancyApplicationService.getApplicationsByVacancyId(vacancyId));
    }

    @Operation(summary = "Cancel vacancy application")
    @ApiResponse(responseCode = "204", description = "Application cancelled successfully")
    @ApiResponse(responseCode = "404", description = "Active application not found")
    @PatchMapping("{vacancyId}/applications/cancel")
    public ResponseEntity<Void> cancelVacancyApplication(@PathVariable Long vacancyId) {
        vacancyApplicationService.cancelVacancyApplication(vacancyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-applications")
    public ResponseEntity<List<VacancyApplicationResponseDto>> getMyApplications() {
        return ResponseEntity.ok(vacancyApplicationService.getMyApplications());
    }

    @Operation(summary = "Update application status (accept/reject)")
    @ApiResponse(responseCode = "200", description = "Application status updated")
    @PatchMapping("/applications/{applicationId}/status")
    public ResponseEntity<VacancyApplicantResponseDto> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestParam ApplicationStatus status
    ) {
        return ResponseEntity.ok(vacancyApplicationService.updateApplicationStatus(applicationId, status));
    }
}