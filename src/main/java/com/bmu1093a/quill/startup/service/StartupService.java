package com.bmu1093a.quill.startup.service;

import com.bmu1093a.quill.auth.model.entity.User;
import com.bmu1093a.quill.common.exception.ResourceNotFoundException;
import com.bmu1093a.quill.common.exception.UnauthorizedActionException;
import com.bmu1093a.quill.startup.mapper.StartupMapper;
import com.bmu1093a.quill.startup.model.dto.request.StartupRequestDto;
import com.bmu1093a.quill.startup.model.dto.request.StartupUpdateRequestDto;
import com.bmu1093a.quill.startup.model.dto.respone.StartupResponseDto;
import com.bmu1093a.quill.startup.model.entity.Startup;
import com.bmu1093a.quill.startup.repository.StartupRepository;
import com.bmu1093a.quill.vacancy.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StartupService {

    private final StartupRepository startupRepository;
    private final UserLookupService userLookupService;
    private final StartupMapper startupMapper;

    private User getCurrentUserOrNull() {
        try {
            return userLookupService.getCurrentUser();
        } catch (UnauthorizedActionException e) {
            return null;
        }
    }

    public List<StartupResponseDto> getAllStartups() {
        return startupRepository.findAll()
                .stream().map(startupMapper::toDto).toList();
    }

    public StartupResponseDto getStartupById(Long id) {
        Startup startup = startupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Startup not found"));

        StartupResponseDto dto = startupMapper.toDto(startup);

        User currentUser = getCurrentUserOrNull();
        boolean isOwner = currentUser != null &&
                startup.getOwner().getId().equals(currentUser.getId());
        dto.setIsOwner(isOwner);

        return dto;
    }

    public StartupResponseDto createStartup(StartupRequestDto startupRequestDto) {
        Startup startup = startupMapper.toStartup(startupRequestDto);
        startup.setOwner(userLookupService.getCurrentUser());
        startupRepository.save(startup);
        return startupMapper.toDto(startup);
    }

    public StartupResponseDto updateStartup(Long id, StartupUpdateRequestDto startupUpdateRequestDto) {
        Startup startup = startupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Startup not found"));

        User currentUser = userLookupService.getCurrentUser();
        if (!startup.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("You are not the owner of this startup");
        }

        startup.setName(startupUpdateRequestDto.getName());
        startup.setTagline(startupUpdateRequestDto.getTagline());
        startup.setDescription(startupUpdateRequestDto.getDescription());
        startup.setCategory(startupUpdateRequestDto.getCategory());
        startup.setStage(startupUpdateRequestDto.getStage());
        startup.setWebsite(startupUpdateRequestDto.getWebsite());
        startup.setIsActive(startupUpdateRequestDto.getIsActive());

        startupRepository.save(startup);

        StartupResponseDto dto = startupMapper.toDto(startup);
        dto.setIsOwner(true);
        return dto;
    }

    public List<StartupResponseDto> getMyStartups() {
        return startupRepository.findByOwner(userLookupService.getCurrentUser())
                .stream().map(startupMapper::toDto).toList();
    }

    public void deleteStartup(Long id) {
        Startup startup = startupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Startup not found"));

        User currentUser = userLookupService.getCurrentUser();
        if (!startup.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("You are not the owner of this startup");
        }

        startupRepository.delete(startup);
    }
}