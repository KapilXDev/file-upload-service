package com.kapil.fileupload.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kapil.fileupload.model.FileVersion;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    // Get all versions of a file, ordered by time (latest first)
    List<FileVersion> findByFileIdOrderByUploadedAtDesc(Long fileId);

    // (Optional) Find a specific version by versionId
    FileVersion findByVersionId(String versionId);
}
