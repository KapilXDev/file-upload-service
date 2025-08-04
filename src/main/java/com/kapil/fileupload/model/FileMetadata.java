package com.kapil.fileupload.model;

import lombok.*;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "file_metadata")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFileName;
    private String storedFileName;
    private String fileType;
    private long fileSize;
    private int version;
    private String uploadedBy;
    private LocalDateTime uploadTime;

    // ✅ Custom constructor matching JPQL projection
    public FileMetadata(String originalFileName, String fileType, long fileSize, String uploadedBy, int version) {
        this.originalFileName = originalFileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.version = version;
    }
}
