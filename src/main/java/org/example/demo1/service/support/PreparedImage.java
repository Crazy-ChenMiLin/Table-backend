package org.example.demo1.service.support;

public record PreparedImage(
        String contentType,
        long sizeBytes,
        byte[] bytes,
        String dataUri
) {
}
