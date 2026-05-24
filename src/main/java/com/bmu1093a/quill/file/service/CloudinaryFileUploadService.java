package com.bmu1093a.quill.file.service;

import com.bmu1093a.quill.auth.model.entity.User;
import com.bmu1093a.quill.auth.repository.UserRepository;
import com.bmu1093a.quill.common.exception.FileOperationException;
import com.bmu1093a.quill.common.exception.FileValidationException;
import com.bmu1093a.quill.file.model.dto.FileUploadResponse;
import com.bmu1093a.quill.file.model.entity.FileRecord;
import com.bmu1093a.quill.file.repo.FileRecordRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryFileUploadService implements FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryFileUploadService.class);

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "application/x-tika-ooxml",
            "application/x-tika-msoffice"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10000;
    private static final String EXT_PDF = ".pdf";
    private static final String EXT_DOCX = ".docx";
    private static final String EXT_DOC = ".doc";
    private static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";
    private static final String MIME_WORDPROCESSING = "wordprocessingml";
    private static final String MIME_MSWORD = "msword";

    private final Cloudinary cloudinary;
    private final Tika tika;
    private final FileRecordRepository fileRecordRepository;
    private final UserRepository userRepository;

    public CloudinaryFileUploadService(Cloudinary cloudinary,
                                       FileRecordRepository fileRecordRepository,
                                       UserRepository userRepository) {
        this.cloudinary = cloudinary;
        this.fileRecordRepository = fileRecordRepository;
        this.userRepository = userRepository;
        this.tika = new Tika();
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public FileUploadResponse uploadFile(MultipartFile file) throws IOException {
        byte[] fileBytes = validateAndGetBytes(file);

        String email = getCurrentUserEmail();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new FileOperationException("USER_NOT_FOUND", "User not found: " + email));

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        String extension = getFileExtension(originalName);
        String publicIdWithExtension = "cv_" + UUID.randomUUID().toString().replace("-", "") + extension;

        Map<String, Object> uploadResult;
        try {
            uploadResult = cloudinary.uploader().upload(
                    fileBytes,
                    ObjectUtils.asMap(
                            "folder", "quill/cv",
                            "resource_type", "raw",
                            "public_id", publicIdWithExtension,
                            "type", "upload",
                            "access_mode", "public"
                    )
            );
        } catch (Exception e) {
            log.error("Cloudinary upload failed", e);
            throw new FileOperationException("UPLOAD_FAILED", "Failed to upload file to cloud storage");
        }

        FileRecord fileRecord = FileRecord.builder()
                .url(String.valueOf(uploadResult.get("secure_url")))
                .publicId(String.valueOf(uploadResult.get("public_id")))
                .originalFileName(originalName)
                .user(currentUser)
                .deleted(false)
                .build();

        fileRecordRepository.save(fileRecord);

        return new FileUploadResponse(fileRecord.getUrl(), fileRecord.getPublicId(), fileRecord.getOriginalFileName());
    }

    @Override
    @Transactional
    public void deleteFile(String publicId) {
        String email = getCurrentUserEmail();
        FileRecord fileRecord = fileRecordRepository
                .findByPublicIdAndDeletedFalse(publicId)
                .orElseThrow(() -> new FileOperationException(FILE_NOT_FOUND, "File not found: " + publicId));

        if (!fileRecord.getUser().getEmail().equals(email)) {
            throw new FileOperationException("ACCESS_DENIED", "Permission denied");
        }

        try {
            cloudinary.uploader().destroy(fileRecord.getPublicId(), ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary: {}", publicId, e);
            throw new FileOperationException("DELETE_FAILED", "Failed to delete file from cloud storage");
        }

        fileRecord.setDeleted(true);
        fileRecordRepository.save(fileRecord);
    }

    @Override
    public ResponseEntity<byte[]> previewCv() throws IOException {
        String email = getCurrentUserEmail();
        FileRecord fileRecord = fileRecordRepository
                .findFirstByUserEmailAndDeletedFalseOrderByIdDesc(email)
                .orElseThrow(() -> new FileOperationException(FILE_NOT_FOUND, "No active CV found in your profile"));

        return downloadAndPrepareResponse(fileRecord);
    }

    @Override
    public FileUploadResponse getCvByUserId(Long userId) {
        FileRecord fileRecord = fileRecordRepository
                .findFirstByUserIdAndDeletedFalseOrderByIdDesc(userId)
                .orElseThrow(() -> new FileOperationException(FILE_NOT_FOUND, "No CV found for this user"));

        return new FileUploadResponse(fileRecord.getUrl(), fileRecord.getPublicId(), fileRecord.getOriginalFileName());
    }

    @Override
    public FileUploadResponse getLastUploadedCv() {
        String email = getCurrentUserEmail();
        FileRecord fileRecord = fileRecordRepository
                .findFirstByUserEmailAndDeletedFalseOrderByIdDesc(email)
                .orElseThrow(() -> new FileOperationException(FILE_NOT_FOUND, "No active CV found in your profile"));

        return new FileUploadResponse(fileRecord.getUrl(), fileRecord.getPublicId(), fileRecord.getOriginalFileName());
    }

    private ResponseEntity<byte[]> downloadAndPrepareResponse(FileRecord fileRecord) throws IOException {
        String url = fileRecord.getUrl();
        String fileName = fileRecord.getOriginalFileName();

        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "cv_document";
        }

        byte[] bytes = downloadFromUrl(url);
        MediaType contentType = determineContentType(fileName);
        String finalFileName = ensureExtension(fileName, contentType);


        String asciiName = finalFileName.replaceAll("[^\\x00-\\x7F]", "_");
        String encodedName = java.net.URLEncoder.encode(finalFileName, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
        String contentDisposition = "attachment; filename=\"" + asciiName + "\"; filename*=UTF-8''" + encodedName;

        return ResponseEntity.ok()
                .contentType(contentType)
                .header("Content-Disposition", contentDisposition)
                .body(bytes);
    }

    private byte[] downloadFromUrl(String urlString) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(urlString).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            log.error("Cloudinary returned error code: {}", responseCode);
            throw new FileOperationException("DOWNLOAD_FAILED", "File not found in cloud storage");
        }

        try (InputStream is = connection.getInputStream()) {
            return is.readAllBytes();
        }
    }

    private MediaType determineContentType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(EXT_PDF)) {
            return MediaType.APPLICATION_PDF;
        } else if (lowerName.endsWith(EXT_DOCX)) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } else if (lowerName.endsWith(EXT_DOC)) {
            return MediaType.parseMediaType("application/msword");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String ensureExtension(String fileName, MediaType contentType) {
        if (fileName.contains(".")) {
            return fileName;
        }
        if (contentType.equals(MediaType.APPLICATION_PDF)) {
            return fileName + EXT_PDF;
        } else if (contentType.toString().contains(MIME_WORDPROCESSING)) {
            return fileName + EXT_DOCX;
        } else if (contentType.toString().contains(MIME_MSWORD)) {
            return fileName + EXT_DOC;
        }
        return fileName;
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new FileOperationException("UNAUTHORIZED", "Authentication required");
        }
        return auth.getName();
    }

    private byte[] validateAndGetBytes(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("FILE_EMPTY", "File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileValidationException("FILE_TOO_LARGE", "Maximum file size is 5MB");
        }

        byte[] bytes = file.getBytes();
        String detectedType;
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            detectedType = tika.detect(is);
        }

        String originalFilename = file.getOriginalFilename();
        String fileName = originalFilename != null ? originalFilename.toLowerCase() : "";
        boolean isAllowedMime = ALLOWED_MIME_TYPES.contains(detectedType);
        boolean isAllowedExtension = fileName.endsWith(EXT_PDF) || fileName.endsWith(EXT_DOCX) || fileName.endsWith(EXT_DOC);

        if (!isAllowedMime && !isAllowedExtension) {
            throw new FileValidationException("INVALID_FILE_TYPE", "Invalid file type detected: " + detectedType);
        }

        return bytes;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}