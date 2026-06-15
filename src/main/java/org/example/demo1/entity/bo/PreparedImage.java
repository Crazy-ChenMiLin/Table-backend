package org.example.demo1.entity.bo;

public record PreparedImage(
        String contentType,
        long sizeBytes,
        byte[] bytes,
        String dataUri
) {
}
