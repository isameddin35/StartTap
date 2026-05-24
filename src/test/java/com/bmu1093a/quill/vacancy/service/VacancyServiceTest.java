package com.bmu1093a.quill.vacancy.service;

import com.bmu1093a.quill.auth.model.entity.User;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VacancyServiceTest {

    @Mock
    private VacancyRepository vacancyRepository;

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private StartupRepository startupRepository;

    @Mock
    private VacancyMapper vacancyMapper;

    @Mock
    private VacancyApplicationRepository vacancyApplicationRepository;

    @InjectMocks
    private VacancyService vacancyService;

    private User user(Long id) {
        return User.builder().id(id).build();
    }

    private Startup startup() {
        Startup s = new Startup();
        s.setOwner(user(1L));
        return s;
    }

    private Vacancy vacancy() {
        Vacancy v = new Vacancy();
        Startup s = new Startup();
        s.setOwner(user(1L));
        v.setStartup(s);
        return v;
    }

    // ---------------- GET VACANCY ----------------

    @Test
    void getVacancy_owner_true() {
        User user = user(1L);
        Vacancy vacancy = vacancy();

        when(vacancyRepository.findById(1L))
                .thenReturn(Optional.of(vacancy));

        when(vacancyMapper.toDto(vacancy))
                .thenReturn(new VacancyResponseDto());

        when(userLookupService.getCurrentUser())
                .thenReturn(user);

        VacancyResponseDto result = vacancyService.getVacancy(1L);

        assertTrue(result.getIsOwner());
    }

    @Test
    void getVacancy_owner_false() {
        User user = user(2L);
        Vacancy vacancy = vacancy();

        when(vacancyRepository.findById(1L))
                .thenReturn(Optional.of(vacancy));

        when(vacancyMapper.toDto(vacancy))
                .thenReturn(new VacancyResponseDto());

        when(userLookupService.getCurrentUser())
                .thenReturn(user);

        VacancyResponseDto result = vacancyService.getVacancy(1L);

        assertFalse(result.getIsOwner());
    }

    @Test
    void getVacancy_shouldThrow_whenNotFound() {
        when(vacancyRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> vacancyService.getVacancy(1L));
    }

    // ---------------- GET BY STARTUP ----------------

    @Test
    void getVacancyByStartupId_success() {
        Vacancy v = new Vacancy();

        when(vacancyRepository.findByStartupId(1L))
                .thenReturn(List.of(v));

        when(vacancyMapper.toDto(any()))
                .thenReturn(new VacancyResponseDto());

        List<VacancyResponseDto> result =
                vacancyService.getVacancyByStartupId(1L);

        assertEquals(1, result.size());
    }

    // ---------------- GET ALL ----------------

    @Test
    void getAllVacancies_success() {
        when(vacancyRepository.findAll())
                .thenReturn(List.of(new Vacancy(), new Vacancy()));

        when(vacancyMapper.toDto(any()))
                .thenReturn(new VacancyResponseDto());

        List<VacancyResponseDto> result =
                vacancyService.getAllVacancies();

        assertEquals(2, result.size());
    }

    // ---------------- CREATE ----------------

    @Test
    void createVacancy_success() {
        User user = user(1L);

        Startup startup = startup();

        VacancyRequestDto dto = new VacancyRequestDto();
        dto.setStartupId(1L);

        when(userLookupService.getCurrentUser())
                .thenReturn(user);

        when(startupRepository.findById(1L))
                .thenReturn(Optional.of(startup));

        when(vacancyRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(vacancyMapper.toDto(any()))
                .thenReturn(new VacancyResponseDto());

        VacancyResponseDto result =
                vacancyService.createVacancy(dto);

        assertNotNull(result);
        verify(vacancyRepository).save(any(Vacancy.class));
    }

    @Test
    void createVacancy_shouldThrow_whenNotOwner() {
        User user = user(2L);

        Startup startup = startup();

        VacancyRequestDto dto = new VacancyRequestDto();
        dto.setStartupId(1L);

        when(userLookupService.getCurrentUser())
                .thenReturn(user);

        when(startupRepository.findById(1L))
                .thenReturn(Optional.of(startup));

        assertThrows(RuntimeException.class,
                () -> vacancyService.createVacancy(dto));
    }

    // ---------------- UPDATE ----------------

    @Test
    void updateVacancy_success() {
        User user = user(1L);

        Vacancy vacancy = vacancy();

        VacancyUpdateRequestDto dto = new VacancyUpdateRequestDto();
        dto.setTitle("New Title");

        when(userLookupService.getCurrentUser())
                .thenReturn(user);

        when(vacancyRepository.findById(1L))
                .thenReturn(Optional.of(vacancy));

        when(vacancyRepository.save(any()))
                .thenReturn(vacancy);

        when(vacancyMapper.toDto(any()))
                .thenReturn(new VacancyResponseDto());

        VacancyResponseDto result =
                vacancyService.updateVacancy(1L, dto);

        assertNotNull(result);
        assertEquals("New Title", vacancy.getTitle());
    }

    @Test
    void updateVacancy_shouldThrow_whenNotOwner() {
        User user = user(2L);

        Vacancy vacancy = vacancy();

        VacancyUpdateRequestDto dto = new VacancyUpdateRequestDto();

        when(userLookupService.getCurrentUser())
                .thenReturn(user);

        when(vacancyRepository.findById(1L))
                .thenReturn(Optional.of(vacancy));

        assertThrows(RuntimeException.class,
                () -> vacancyService.updateVacancy(1L, dto));
    }
    @Test
    void getVacancy_unauthenticated_isOwner_false() {
        Vacancy vacancy = vacancy();
        when(vacancyRepository.findById(1L)).thenReturn(Optional.of(vacancy));
        when(vacancyMapper.toDto(vacancy)).thenReturn(new VacancyResponseDto());
        when(userLookupService.getCurrentUser())
                .thenThrow(new UnauthorizedActionException("Auth required"));

        VacancyResponseDto result = vacancyService.getVacancy(1L);

        assertFalse(result.getIsOwner());
    }
}