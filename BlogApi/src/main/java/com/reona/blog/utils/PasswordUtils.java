package com.reona.blog.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.reona.blog.commcn.Constants;
import com.reona.blog.exception.ServiceException;

import java.nio.charset.StandardCharsets;

public final class PasswordUtils {

    private static final int MIN_LENGTH = 6;
    private static final int MAX_BYTES = 72;

    private PasswordUtils() {
    }

    public static String hash(String raw) {
        return BCrypt.hashpw(raw, BCrypt.gensalt());
    }

    public static boolean matches(String raw, String stored) {
        if (StrUtil.isBlank(raw) || StrUtil.isBlank(stored)) {
            return false;
        }
        if (isHashed(stored)) {
            try {
                return BCrypt.checkpw(raw, stored);
            } catch (Exception e) {
                return false;
            }
        }
        return stored.equals(raw);
    }

    public static boolean isHashed(String stored) {
        return stored != null && stored.matches("^\\$2[aby]\\$.*");
    }

    public static void validate(String raw) {
        if (raw == null || raw.length() < MIN_LENGTH
                || raw.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new ServiceException(Constants.CODE_400, "密码至少 6 个字符，且不能超过 72 字节");
        }
    }
}
