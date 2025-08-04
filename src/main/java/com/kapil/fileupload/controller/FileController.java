package com.kapil.fileupload.controller;

import com.kapil.fileupload.model.FileEntity;
import com.kapil.fileupload.model.FileMetadata;
import com.kapil.fileupload.model.FileVersion;
import com.kapil.fileupload.repo.FileRepository;
import com.kapil.fileupload.repo.FileVersionRepository;
import com.kapil.fileupload.service.FileServiceImpl;
import com.kapil.fileupload.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final FileServiceImpl fileService;
    private final FileRepository fileEntityRepository;
    private final FileVersionRepository fileVersionRepository;

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_" + role));
    }

    @Operation(summary = "Upload a file with versioning support")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        Authentication auth) {
        String username = auth.getName();
        try {
            String storedFileName = fileStorageService.storeFile(file, username);
            return ResponseEntity.ok("File uploaded successfully as: " + storedFileName);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Download a file by stored filename")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/download")
    public ResponseEntity<?> downloadByStoredFileName(@RequestParam("storedFileName") String storedFileName,
                                                      Authentication auth) {
        Optional<FileEntity> fileEntityOpt = fileService.getFileByStoredName(storedFileName);
        if (fileEntityOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found for storedFileName.");
        }

        FileEntity fileEntity = fileEntityOpt.get();
        String username = auth.getName();

        if (!fileEntity.getOwner().equals(username) && !hasRole(auth, "ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        FileVersion fileVersion = fileEntity.getVersions().stream()
                .filter(v -> storedFileName.equals(v.getFile().getStoredFileName()))
                .findFirst().orElse(null);

        if (fileVersion == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No file version matches the storedFileName.");
        }

        Resource resource = fileStorageService.loadFileAsResource(storedFileName);
        if (resource == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Physical file not found on disk.");
        }

        String encodedFileName = URLEncoder.encode(fileEntity.getFileName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                .body(resource);
    }

    @Operation(summary = "Delete a specific stored file")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteByStoredFileName(@RequestParam("storedFileName") String storedFileName,
                                                    Authentication auth) {
        String username = auth.getName();
        Optional<FileEntity> fileEntityOpt = fileService.getFileByStoredName(storedFileName, username);

        if (fileEntityOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found for storedFileName.");
        }

        FileEntity fileEntity = fileEntityOpt.get();

        if (!fileEntity.getOwner().equals(username) && !hasRole(auth, "ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        Optional<FileVersion> versionOpt = fileEntity.getVersions().stream()
                .filter(v -> storedFileName.equals(v.getFile().getStoredFileName()))
                .findFirst();

        if (versionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Matching version not found.");
        }

        FileVersion version = versionOpt.get();

        try {
            boolean deleted = fileStorageService.deleteFile(storedFileName, username);
            if (deleted) {
                fileEntity.getVersions().remove(version);
                fileVersionRepository.delete(version);

                if (fileEntity.getVersions().isEmpty()) {
                    fileEntityRepository.delete(fileEntity);
                    return ResponseEntity.ok("Last version deleted. File entry removed.");
                } else {
                    fileEntity.setCurrentVersion(fileEntity.getCurrentVersion() - 1);
                    fileEntityRepository.save(fileEntity);
                }

                return ResponseEntity.ok("Version deleted successfully.");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File deletion failed.");
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during deletion: " + e.getMessage());
        }
    }

/*    @Operation(summary = "Get metadata of all versions of a file")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/versions")
    public ResponseEntity<?> getFileVersions(@RequestParam("filename") String filename,
                                             Authentication auth) {
        String username = auth.getName();
        Optional<FileEntity> fileEntityOpt = fileEntityRepository.findByFileNameAndOwner(filename, username);
        if (fileEntityOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found.");
        }

        FileEntity fileEntity = fileEntityOpt.get();
        if (!fileEntity.getOwner().equals(username) && !hasRole(auth, "ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        List<Map<String, Object>> versionList = new ArrayList<>();
        for (FileVersion version : fileEntity.getVersions()) {
            Map<String, Object> versionData = new LinkedHashMap<>();
            versionData.put("version", version.getVersionId());
            versionData.put("size", version.getSize());
            versionData.put("uploadedBy", version.getUploadedBy());
            versionData.put("uploadedAt", version.getUploadedAt());
            versionList.add(versionData);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("file", fileEntity.getFileName());
        response.put("owner", fileEntity.getOwner());
        response.put("totalVersions", versionList.size());
        response.put("versions", versionList);

        return ResponseEntity.ok(response);
    }*/

    @Operation(summary = "Get all uploaded files for a user")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/files", params = "username")
    public ResponseEntity<List<FileMetadata>> getAllFilesByUser(@RequestParam("username") String username) {
        List<FileMetadata> files = fileStorageService.getAllFilesByUser(username);
        return ResponseEntity.ok(files);
    }

    @Operation(summary = "Get all uploaded files")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getAll")
    public ResponseEntity<List<FileMetadata>> getAllFiles() {
        List<FileMetadata> files = fileStorageService.getAllFiles();
        return ResponseEntity.ok(files);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/files/{fileName}/versions")
    public ResponseEntity<List<FileVersion>> getFileVersions(@PathVariable String fileName,
                                                             Authentication auth) {
        FileEntity file = fileEntityRepository.findByFileNameAndOwner(fileName, auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        if (!file.getOwner().equals(auth.getName()) && !hasRole(auth, "ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(fileService.getFileVersions(fileName));
    }
}
