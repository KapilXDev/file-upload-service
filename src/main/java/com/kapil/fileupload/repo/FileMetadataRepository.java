package com.kapil.fileupload.repo;

import com.kapil.fileupload.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findByOriginalFileNameOrderByVersionDesc(String fileName);
    Optional<FileMetadata> findTopByOriginalFileNameOrderByVersionDesc(String fileName);
    
    @Query("SELECT new com.kapil.fileupload.model.FileMetadata(f.fileName, f.fileType, f.size, f.owner, f.currentVersion) FROM FileEntity f")
    List<FileMetadata> findAll();
}