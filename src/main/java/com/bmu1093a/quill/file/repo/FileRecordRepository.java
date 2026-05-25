package com.bmu1093a.quill.file.repo;

import com.bmu1093a.quill.file.model.entity.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {
    Optional<FileRecord> findByPublicIdAndDeletedFalse(String publicId);
    Optional<FileRecord> findFirstByUserEmailAndDeletedFalseOrderByIdDesc(String email);
    Optional<FileRecord> findFirstByUserIdAndDeletedFalseOrderByIdDesc(Long userId);
}