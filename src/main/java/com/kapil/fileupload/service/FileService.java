package com.kapil.fileupload.service;

import com.kapil.fileupload.model.FileEntity;
import com.kapil.fileupload.model.FileVersion;

import java.util.List;
import java.util.Optional;

public interface FileService {

    /**
     * Retrieves a file entity by stored file name.
     *
     * @param storedFileName stored (UUID) name
     * @return Optional of FileEntity
     */
    Optional<FileEntity> getFileByStoredName(String storedFileName);

    /**
     * Retrieves a file entity by stored file name and username.
     *
     * @param storedFileName stored (UUID) name
     * @param username file owner
     * @return Optional of FileEntity
     */
    Optional<FileEntity> getFileByStoredName(String storedFileName, String username);

    /**
     * Saves a versioned file or updates existing file with a new version.
     *
     * @param fileName original file name
     * @param contentType MIME type
     * @param size file size in bytes
     * @param username file uploader
     * @param storedName stored (UUID) name
     * @param filePath path on disk
     * @return saved FileEntity
     */
    FileEntity saveVersionedFile(String fileName, String contentType, long size, String username, String storedName, String filePath);

    /**
     * Deletes a file entity using stored name and username.
     *
     * @param storedFileName stored (UUID) name
     * @param username file owner
     */
    void deleteFile(String storedFileName, String username);
    
    List<FileVersion> getFileVersions(String filename);
}
