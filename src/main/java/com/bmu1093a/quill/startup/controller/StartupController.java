package com.bmu1093a.quill.startup.controller;

import com.bmu1093a.quill.startup.model.dto.request.StartupRequestDto;
import com.bmu1093a.quill.startup.model.dto.request.StartupUpdateRequestDto;
import com.bmu1093a.quill.startup.model.dto.respone.StartupResponseDto;
import com.bmu1093a.quill.startup.service.StartupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/startups")
@RequiredArgsConstructor
public class StartupController {

    private final StartupService startupService;

    @GetMapping
    public ResponseEntity<List<StartupResponseDto>> getAllStartups() {
        return ResponseEntity.ok(startupService.getAllStartups());
    }

    @GetMapping("{id}")
    public StartupResponseDto getStartupById(@PathVariable Long id) {
        return startupService.getStartupById(id);
    }

    @PostMapping
    public ResponseEntity<StartupResponseDto> createStartup(@RequestBody StartupRequestDto startupRequestDto) {
        StartupResponseDto startup = startupService.createStartup(startupRequestDto);
        return ResponseEntity.ok(startup);
    }

    @PutMapping("{id}")
    public StartupResponseDto updateStartup(
            @RequestBody StartupUpdateRequestDto startupUpdateRequestDto,
            @PathVariable Long id) {
        return startupService.updateStartup(id, startupUpdateRequestDto);
    }

    @GetMapping("/me")
    public ResponseEntity<List<StartupResponseDto>> getMyStartups() {
        return ResponseEntity.ok(startupService.getMyStartups());
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteStartup(@PathVariable Long id) {
        startupService.deleteStartup(id);
        return ResponseEntity.noContent().build();
    }

}
