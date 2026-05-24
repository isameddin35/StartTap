package com.bmu1093a.quill.vacancy.service;

import com.bmu1093a.quill.auth.model.entity.User;
import com.bmu1093a.quill.common.exception.AlreadyAppliedException;
import com.bmu1093a.quill.common.exception.ResourceNotFoundException;
import com.bmu1093a.quill.common.exception.UnauthorizedActionException;
import com.bmu1093a.quill.common.exception.VacancyNotActiveException;
import com.bmu1093a.quill.vacancy.mapper.VacancyApplicationMapper;
import com.bmu1093a.quill.vacancy.model.dto.response.VacancyApplicantResponseDto;
import com.bmu1093a.quill.vacancy.model.dto.response.VacancyApplicationResponseDto;
import com.bmu1093a.quill.vacancy.model.entity.Vacancy;
import com.bmu1093a.quill.vacancy.model.entity.VacancyApplication;
import com.bmu1093a.quill.vacancy.model.entity.enumeration.ApplicationStatus;
import com.bmu1093a.quill.vacancy.respository.VacancyApplicationRepository;
import com.bmu1093a.quill.vacancy.respository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VacancyApplicationService {

    private final VacancyApplicationRepository vacancyApplicationRepository;
    private final VacancyRepository vacancyRepository;
    private final VacancyApplicationMapper vacancyApplicationMapper;
    private final UserLookupService userLookupService;

    public VacancyApplicationResponseDto applyToVacancy(
//            VacancyApplicationRequestDto vacancyApplicationRequestDto
            Long vacancyId
    ) {
        User currentUser = userLookupService.getCurrentUser();

        Long userId = currentUser.getId();

        Vacancy vacancy = vacancyRepository.findById(vacancyId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found"));

        if (Boolean.FALSE.equals(vacancy.getIsActive())) {
            throw new VacancyNotActiveException("Vacancy is not active");
        }
        boolean alreadyApplied = vacancyApplicationRepository
                .existsByUser_IdAndVacancy_IdAndStatusIn(
                        userId, vacancyId,
                        List.of(ApplicationStatus.PENDING, ApplicationStatus.ACCEPTED)
                );

        if (alreadyApplied) {
            throw new AlreadyAppliedException("Already applied");
        }

        VacancyApplication vacancyApplication = VacancyApplication.builder()
                .user(currentUser)
                .vacancy(vacancy)
                .build();

        vacancyApplicationRepository.save(vacancyApplication);

        return vacancyApplicationMapper.toVacancyApplicationResponseDto(vacancyApplication);

    }

    public List<VacancyApplicantResponseDto> getApplicationsByVacancyId(Long id) {

        List<VacancyApplication> vacancyApplications = vacancyApplicationRepository.findVacancyApplicationsByVacancyId(id);

        return vacancyApplications.stream().map(vacancyApplicationMapper::toVacancyApplicantDtoResponse).toList();
    }

    public void cancelVacancyApplication(Long vacancyId) {
        User currentUser = userLookupService.getCurrentUser();

        VacancyApplication application = vacancyApplicationRepository
                .findByVacancy_IdAndUser_IdAndStatus(
                        vacancyId, currentUser.getId(), ApplicationStatus.PENDING
                )
                .orElseThrow(() -> new ResourceNotFoundException("Active application not found"));

        application.setStatus(ApplicationStatus.CANCELED);
        vacancyApplicationRepository.save(application);
    }

    public List<VacancyApplicationResponseDto> getMyApplications() {
        User currentUser = userLookupService.getCurrentUser();
        return vacancyApplicationRepository.findByUser(currentUser)
                .stream()
                .map(vacancyApplicationMapper::toVacancyApplicationResponseDto)
                .toList();
    }

    public VacancyApplicantResponseDto updateApplicationStatus(Long applicationId, ApplicationStatus newStatus) {
        User currentUser = userLookupService.getCurrentUser();

        VacancyApplication application = vacancyApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING applications can be updated");
        }

        Vacancy vacancy = application.getVacancy();
        if (vacancy.getStartup() == null || !vacancy.getStartup().getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("Only the startup owner can accept/reject applications");
        }

        if (newStatus != ApplicationStatus.ACCEPTED && newStatus != ApplicationStatus.REJECTED) {
            throw new IllegalArgumentException("Status must be ACCEPTED or REJECTED");
        }

        application.setStatus(newStatus);
        vacancyApplicationRepository.save(application);

        return vacancyApplicationMapper.toVacancyApplicantDtoResponse(application);
    }

}
