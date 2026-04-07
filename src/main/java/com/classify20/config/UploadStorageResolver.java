package com.classify20.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class UploadStorageResolver {

    private final String configuredUploadPath;
    private volatile Path cachedRootPath;

    public UploadStorageResolver(@Value("${classify.upload.path:uploads}") String configuredUploadPath) {
        this.configuredUploadPath = configuredUploadPath;
    }

    public Path resolveRootPath() {
        Path cached = cachedRootPath;
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            if (cachedRootPath != null) {
                return cachedRootPath;
            }

            cachedRootPath = determineWritableRoot();
            return cachedRootPath;
        }
    }

    public Path resolveSubdirectory(String folderName) throws IOException {
        Path directory = resolveRootPath().resolve(folderName).normalize();
        Files.createDirectories(directory);
        return directory;
    }

    public String toResourceLocation() {
        return resolveRootPath().toUri().toString();
    }

    private Path determineWritableRoot() {
        Path preferredRoot = buildPath(configuredUploadPath);

        try {
            return ensureWritableDirectory(preferredRoot);
        } catch (IOException ignored) {
            Path fallbackRoot = buildPath("uploads");

            try {
                return ensureWritableDirectory(fallbackRoot);
            } catch (IOException fallbackException) {
                throw new IllegalStateException("No se pudo preparar el directorio de uploads.", fallbackException);
            }
        }
    }

    private Path buildPath(String rawPath) {
        String trimmedPath = rawPath == null ? "" : rawPath.trim();
        if (trimmedPath.isBlank()) {
            trimmedPath = "uploads";
        }

        return Paths.get(trimmedPath).toAbsolutePath().normalize();
    }

    private Path ensureWritableDirectory(Path directory) throws IOException {
        Files.createDirectories(directory);

        if (!Files.isDirectory(directory) || !Files.isWritable(directory)) {
            throw new IOException("El directorio no es escribible: " + directory);
        }

        return directory;
    }
}
