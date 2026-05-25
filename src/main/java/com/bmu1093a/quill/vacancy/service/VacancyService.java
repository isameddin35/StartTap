package com.bmu1093a.quill.vacancy.service;

import com.bmu1093a.quill.auth.model.entity.User;
import com.bmu1093a.quill.common.exception.ResourceNotFoundException;
import com.bmu1093a.quill.common.exception.UnauthorizedActionException;
import com.bmu1093a.quill.startup.model.entity.Startup;
import com.bmu1093a.quill.startup.repository.StartupRepository;
import com.bmu1093a.quill.vacancy.mapper.VacancyMapper;
import com.bmu1093a.quill.vacancy.model.dto.request.VacancyRequestDto;
import com.bmu1093a.quill.vacancy.model.dto.request.VacancyUpdateRequestDto;
import com.bmu1093a.quill.vacancy.model.dto.response.VacancyResponseDto;
import com.bmu1093a.quill.vacancy.model.entity.Vacancy;
import com.bmu1093a.quill.vacancy.respository.VacancyApplicationRepository;
import com.bmu1093a.quill.vacancy.respository.VacancyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VacancyService {

    private final VacancyRepository vacancyRepository;
    private final UserLookupService userLookupService;
    private final StartupRepository startupRepository;
    private final VacancyMapper vacancyMapper;
    private final VacancyApplicationRepository vacancyApplicationRepository;

    private User getCurrentUserOrNull() {
        try {
            return userLookupService.getCurrentUser();
        } catch (UnauthorizedActionException e) {
            return null;
        }
    }


    public VacancyResponseDto getVacancy(Long id) {

        Vacancy vacancy = vacancyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vacancy not found"));


        VacancyResponseDto vacancyResponseDto = vacancyMapper.toDto(vacancy);

        User currentUser = getCurrentUserOrNull();

        boolean isOwner = false;

        if (currentUser != null) {
            isOwner = vacancy.getStartup().getOwner().getId()
                    .equals(currentUser.getId());
        }

        vacancyResponseDto.setIsOwner(isOwner);
        vacancyResponseDto.setApplicationCount(vacancyApplicationRepository.countByVacancy_Id(id));

        return vacancyResponseDto;

    }

    public List<VacancyResponseDto> getVacancyByStartupId(Long startupId) {
        return vacancyRepository.findByStartupId(startupId).stream().map(v -> {
            VacancyResponseDto dto = vacancyMapper.toDto(v);
            dto.setApplicationCount(vacancyApplicationRepository.countByVacancy_Id(v.getId()));
            return dto;
        }).toList();
    }

    public List<VacancyResponseDto> getAllVacancies() {
        List<Vacancy> vacancies = vacancyRepository.findAll();
        return vacancies.stream().map(v -> {
            VacancyResponseDto dto = vacancyMapper.toDto(v);
            dto.setApplicationCount(vacancyApplicationRepository.countByVacancy_Id(v.getId()));
            return dto;
        }).toList();
    }

    public VacancyResponseDto createVacancy(VacancyRequestDto vacancyRequestDto) {
        User user = userLookupService.getCurrentUser();


        Startup startup = startupRepository.findById(vacancyRequestDto.getStartupId()).orElseThrow(() -> new ResourceNotFoundException("Startup not found"));


        if (!startup.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("You are not the owner of this startup");

        }

        Vacancy vacancy = Vacancy.builder()
                .title(vacancyRequestDto.getTitle())
                .description(vacancyRequestDto.getDescription())
                .salary(vacancyRequestDto.getSalary())
                .startup(startup)
                .build();

        vacancyRepository.save(vacancy);

        return vacancyMapper.toDto(vacancy);
    }

    @Transactional
    public VacancyResponseDto updateVacancy(Long id, VacancyUpdateRequestDto dto) {

        User currentUser = userLookupService.getCurrentUser();

        Vacancy vacancy = vacancyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found"));

        // 🔐 Security check
        if (!vacancy.getStartup().getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("You are not allowed to update this vacancy");
        }

        // ✏️ Update only allowed fields
        if (dto.getTitle() != null) {
            vacancy.setTitle(dto.getTitle());
        }

        if (dto.getDescription() != null) {
            vacancy.setDescription(dto.getDescription());
        }

        if (dto.getSalary() != null) {
            vacancy.setSalary(dto.getSalary());
        }

        if (dto.getIsActive() != null) {
            vacancy.setIsActive(dto.getIsActive());
        }

        vacancyRepository.save(vacancy);

        return vacancyMapper.toDto(vacancy);
    }


}
