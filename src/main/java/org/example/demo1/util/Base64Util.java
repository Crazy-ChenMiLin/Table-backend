package org.example.demo1.util;

import java.util.Base64;

public final class Base64Util {

    private Base64Util() {
    }

    public static String encodeToDataUri(byte[] bytes, String contentType) {
        return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
