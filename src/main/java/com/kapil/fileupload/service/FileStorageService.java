package com.kapil.fileupload.service;

import com.kapil.fileupload.model.FileEntity;
import com.kapil.fileupload.model.FileMetadata;
import com.kapil.fileupload.model.FileVersion;
import com.kapil.fileupload.repo.FileMetadataRepository;
import com.kapil.fileupload.repo.FileRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private final FileServiceImpl fileService;
    private final FileRepository fileRepository;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir, 
    		FileServiceImpl fileService,
            FileRepository fileRepository) throws IOException {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.fileService = fileService;
        this.fileRepository = fileRepository;
        Files.createDirectories(this.fileStorageLocation);
    }

    public String storeFile(MultipartFile file, String username) throws IOException {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + "." + extension;

        Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        fileService.saveVersionedFile(
                originalFileName,
                file.getContentType(),
                file.getSize(),
                username,
                storedFileName,
                targetLocation.toString()
        );

        return storedFileName;
    }

    public Resource loadFileAsResource(String storedFileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(storedFileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            return (resource.exists()) ? resource : null;
        } catch (MalformedURLException ex) {
            return null;
        }
    }


    public boolean deleteFile(String storedFileName, String username) throws IOException {
        Optional<FileEntity> fileEntityOpt = fileService.getFileByStoredName(storedFileName, username);
        if (fileEntityOpt.isEmpty()) {
            throw new IOException("File not found in database for user: " + username);
        }

        // delete all physical file versions
        for (FileVersion version : fileEntityOpt.get().getVersions()) {
            Path path = Paths.get(version.getFilePath());
            Files.deleteIfExists(path);
        }

        // delete metadata from database
        fileService.deleteFile(storedFileName, username);

        return true;
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex >= 0) ? fileName.substring(dotIndex + 1) : "";
    }
    public List<FileMetadata> getAllFiles() {
        List<FileEntity> files = fileRepository.findAll();

        return files.stream()
                .map(this::mapToMetadata)
                .collect(Collectors.toList());
    }
    
    public List<FileMetadata> getAllFilesByUser(String username) {
        List<FileEntity> files = fileRepository.findByOwner(username);

        return files.stream()
                .map(this::mapToMetadata)
                .collect(Collectors.toList());
    }

    private FileMetadata mapToMetadata(FileEntity entity) {
        FileVersion latestVersion = entity.getVersions()
                .stream()
                .max(Comparator.comparing(FileVersion::getUploadedAt))
                .orElse(null);

        return FileMetadata.builder()
                .originalFileName(entity.getFileName())
                .storedFileName(entity.getStoredFileName())
                .fileType(entity.getFileType())
                .fileSize(latestVersion != null ? latestVersion.getSize() : entity.getSize())
                .version(entity.getCurrentVersion())
                .uploadedBy(latestVersion != null ? latestVersion.getUploadedBy() : entity.getOwner())
                .uploadTime(latestVersion != null ? latestVersion.getUploadedAt() : entity.getCreatedAt())
                .build();
    }

}
