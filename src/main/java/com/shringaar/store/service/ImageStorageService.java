package com.shringaar.store.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import com.shringaar.store.config.StoreProperties;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Saves admin-uploaded product photos to disk; swap for S3/Cloudinary in production. */
@Service
public class ImageStorageService {

    private static final Map<String, String> ALLOWED = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");

    private final Path dir;

    public ImageStorageService(StoreProperties props) throws IOException {
        this.dir = Path.of(props.uploadDir()).toAbsolutePath();
        Files.createDirectories(dir);
    }

    /** @return the public URL path, e.g. /uploads/3f2c....jpg */
    public String store(MultipartFile file) throws IOException {
        String ext = ALLOWED.get(file.getContentType());
        if (ext == null) throw new IllegalArgumentException("Upload a JPG, PNG or WebP image");
        String name = UUID.randomUUID() + ext;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }
        return "/uploads/" + name;
    }
}
