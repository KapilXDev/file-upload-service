package com.kapil.fileupload.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kapil.fileupload.model.FileEntity;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByFileNameAndOwner(String fileName, String owner);
    List<FileEntity> findByOwner(String owner);
    Optional<FileEntity> findByFileName(String fileName);
    Optional<FileEntity> findByVersions_File_StoredFileNameAndOwner(String storedFileName, String owner);

}