package com.kapil.fileupload.service;

import com.kapil.fileupload.model.FileEntity;
import com.kapil.fileupload.model.FileVersion;
import com.kapil.fileupload.repo.FileRepository;
import com.kapil.fileupload.repo.FileVersionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService{

    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;

    @Autowired
    public FileServiceImpl(FileRepository fileRepository, FileVersionRepository fileVersionRepository) {
        this.fileRepository = fileRepository;
        this.fileVersionRepository = fileVersionRepository;
    }


    public Optional<FileEntity> getFileByStoredName(String storedFileName) {
        return fileRepository.findAll().stream()
            .filter(file -> file.getVersions().stream()
                .anyMatch(v -> storedFileName.equals(v.getFile().getStoredFileName())))
            .findFirst();
    }


    @Transactional
    public FileEntity saveVersionedFile(String fileName, String contentType, long size, String username, String storedName, String filePath) {
        FileEntity fileEntity = fileRepository.findByFileNameAndOwner(fileName, username).orElse(null);

        FileVersion newVersion = new FileVersion();
        newVersion.setVersionId(UUID.randomUUID().toString());
        newVersion.setFilePath(filePath);
        newVersion.setSize(size);
        newVersion.setUploadedAt(LocalDateTime.now());
        newVersion.setUploadedBy(username);

        if (fileEntity == null) {
            fileEntity = new FileEntity();
            fileEntity.setFileName(fileName);
            fileEntity.setStoredFileName(storedName);
            fileEntity.setFileType(getFileExtension(fileName));
            fileEntity.setContentType(contentType);
            fileEntity.setSize(size);
            fileEntity.setOwner(username);
            fileEntity.setCurrentVersion(1);
            fileEntity.setCreatedAt(LocalDateTime.now());
        } else {
            fileEntity.setCurrentVersion(fileEntity.getCurrentVersion() + 1);
            fileEntity.setStoredFileName(storedName);
            fileEntity.setSize(size);
        }

        newVersion.setFile(fileEntity);
        fileEntity.getVersions().add(newVersion);

        return fileRepository.save(fileEntity);
    }

    public Optional<FileEntity> getFileByStoredName(String storedFileName, String username) {
        return fileRepository.findAll().stream()
                .filter(file -> file.getOwner().equals(username))
                .filter(file -> file.getVersions().stream()
                        .anyMatch(v -> v.getFile().getStoredFileName().equals(storedFileName)))
                .findFirst();
    }

    @Transactional
    public void deleteFile(String storedFileName, String username) {
        Optional<FileEntity> fileOpt = getFileByStoredName(storedFileName, username);
        fileOpt.ifPresent(fileRepository::delete);
    }

    private String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index >= 0 ? fileName.substring(index + 1) : "";
    }


	@Override
	public List<FileVersion> getFileVersions(String filename) {
		// TODO Auto-generated method stub
		return null;
	}
}
