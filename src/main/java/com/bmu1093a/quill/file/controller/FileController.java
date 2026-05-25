package com.bmu1093a.quill.file.controller;

import com.bmu1093a.quill.file.model.dto.FileUploadResponse;
import com.bmu1093a.quill.file.service.FileUploadService;
import com.bmu1093a.quill.vacancy.model.entity.enumeration.ApplicationStatus;
import com.bmu1093a.quill.vacancy.respository.VacancyApplicationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File", description = "CV file upload, retrieval and delete operations")
public class FileController {

    private final FileUploadService fileUploadService;
    private final VacancyApplicationRepository vacancyApplicationRepository;

    @Operation(summary = "Upload CV", description = "Uploads a PDF/DOCX file to Cloudinary and saves record to DB")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file type or size"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Upload failed")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return ResponseEntity.ok(fileUploadService.uploadFile(file));
    }

    @Operation(summary = "Get user's last CV", description = "Fetches the most recent, non-deleted CV for the logged-in user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CV found and returned"),
            @ApiResponse(responseCode = "404", description = "No CV found for this user")
    })
    @GetMapping("/my-cv")
    public ResponseEntity<FileUploadResponse> getMyLastCv() {
        return ResponseEntity.ok(fileUploadService.getLastUploadedCv());
    }

    @Operation(summary = "Delete CV", description = "Soft deletes file from DB using its publicId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @DeleteMapping(value = "/delete", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deleteFile(
            @RequestParam String publicId
    ) {
        fileUploadService.deleteFile(publicId);
        return ResponseEntity.ok("File deleted successfully");
    }
    @Operation(summary = "Preview CV", description = "Proxies the CV file from Cloudinary")
    @GetMapping("/preview-cv")
    public ResponseEntity<byte[]> previewCv() throws IOException {
        return fileUploadService.previewCv();
    }

    @Operation(summary = "Get applicant CV", description = "Returns CV of a user who applied to your vacancy")
    @GetMapping("/user/{userId}/cv")
    public ResponseEntity<FileUploadResponse> getApplicantCv(@PathVariable Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth.getName();

        boolean hasApplication = vacancyApplicationRepository
                .findVacancyApplicationsByUserId(userId)
                .stream()
                .anyMatch(app ->
                        app.getVacancy().getStartup() != null
                        && app.getVacancy().getStartup().getOwner().getEmail().equals(currentEmail)
                        && app.getStatus() == ApplicationStatus.PENDING
                );

        if (!hasApplication) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(fileUploadService.getCvByUserId(userId));
    }
}