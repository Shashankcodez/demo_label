package com.labelcheck.service;

import com.labelcheck.exception.FileStorageException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service managing temporary file storage operations on the server filesystem.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path uploadLocation;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadLocation);
            log.info("Temporary upload storage initialized at configured directory: [{}]", uploadLocation);
        } catch (IOException e) {
            log.error("Failed to initialize upload directory: {}", e.getMessage(), e);
            throw new FileStorageException("Could not initialize storage directory", e);
        }
    }

    /**
     * Safely stores an uploaded file under the configured upload directory with a UUID-based filename.
     *
     * @param file      the uploaded multipart file
     * @param scanId    the unique identifier for the scan session
     * @param extension the canonical extension derived from validated content (e.g. "jpg", "png", "webp")
     * @return the generated safe filename (excluding directory path)
     */
    public String storeFile(MultipartFile file, UUID scanId, String extension) {
        String safeFilename = scanId.toString() + "." + extension;
        Path targetLocation = uploadLocation.resolve(safeFilename).normalize();

        // Path traversal defense check
        if (!targetLocation.startsWith(uploadLocation)) {
            log.warn("Path traversal attempt detected while storing file with name: [{}]", safeFilename);
            throw new FileStorageException("Cannot store file outside current storage directory");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored uploaded file [{}] with size [{}] bytes", safeFilename, file.getSize());
            return safeFilename;
        } catch (IOException e) {
            log.error("I/O error storing file [{}]: {}", safeFilename, e.getMessage(), e);
            throw new FileStorageException("Failed to store uploaded file", e);
        }
    }

    /**
     * Returns the absolute path of the upload location (used for service operations and test verification).
     */
    public Path getUploadLocation() {
        return uploadLocation;
    }
}
