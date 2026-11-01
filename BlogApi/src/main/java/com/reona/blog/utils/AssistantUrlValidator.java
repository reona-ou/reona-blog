package com.reona.blog.utils;

import java.net.URI;
import java.util.Locale;

public final class AssistantUrlValidator {

    private AssistantUrlValidator() {
    }

    public static boolean isValid(String url) {
        if (url == null) {
            return false;
        }
        String value = url.trim();
        if (value.isEmpty() || value.length() > 255 || !value.matches("^https?://\\S+$")) {
            return false;
        }
        try {
            return URI.create(value).getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isBlocked(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) {
                return true;
            }
            String normalized = host.toLowerCase(Locale.ROOT);
            return normalized.equals("169.254.169.254")
                    || normalized.equals("100.100.100.200")
                    || normalized.equals("metadata.google.internal")
                    || normalized.startsWith("169.254.")
                    || normalized.startsWith("[fe80:");
        } catch (Exception e) {
            return true;
        }
    }
}
