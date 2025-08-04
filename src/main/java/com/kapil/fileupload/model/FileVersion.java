package com.kapil.fileupload.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String versionId; // Easier for sorting/comparison
    private String filePath;
    private long size;

    private String uploadedBy; // Optional, if different from original owner
    private LocalDateTime uploadedAt;

    @ManyToOne
    @JoinColumn(name = "file_id")
    private FileEntity file;
}
