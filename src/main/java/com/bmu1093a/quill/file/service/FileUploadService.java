package com.bmu1093a.quill.file.service;

import com.bmu1093a.quill.file.model.dto.FileUploadResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FileUploadService {
    FileUploadResponse uploadFile(MultipartFile file) throws IOException;

    void deleteFile(String publicId);
    ResponseEntity<byte[]> previewCv() throws IOException;
    FileUploadResponse getLastUploadedCv();
    FileUploadResponse getCvByUserId(Long userId);
}