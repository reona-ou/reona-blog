package com.reona.blog.utils;

import cn.hutool.core.util.StrUtil;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;

public final class ClientIpUtils {

    private ClientIpUtils() {
    }

    public static String clientIp(HttpServletRequest request, String trustedProxies) {
        String remote = request.getRemoteAddr();
        if (remote == null) {
            remote = "";
        }
        if (!isTrusted(remote, trustedProxies)) {
            return remote;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwarded)) {
            // 从右向左找第一个非可信代理，避免客户端伪造链首部绕过限流
            String[] chain = forwarded.split(",");
            for (int i = chain.length - 1; i >= 0; i--) {
                String candidate = chain[i].trim();
                if (!candidate.isEmpty() && !isTrusted(candidate, trustedProxies)) {
                    return candidate;
                }
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(realIp)) {
            return realIp.trim();
        }
        return remote;
    }

    public static boolean isPrivateOrLoopback(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(ip.trim());
            if (address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress()) {
                return true;
            }
            byte[] bytes = address.getAddress();
            return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isTrusted(String remote, String trustedProxies) {
        if (StrUtil.isBlank(trustedProxies)) {
            return false;
        }
        for (String proxy : trustedProxies.split(",")) {
            if (remote.equals(proxy.trim())) {
                return true;
            }
        }
        return false;
    }
}
