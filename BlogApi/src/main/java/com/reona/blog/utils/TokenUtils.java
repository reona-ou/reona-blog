package com.reona.blog.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.reona.blog.entity.User;
import com.reona.blog.service.IUserService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;

@Component
public class TokenUtils {

    private static IUserService staticUserService;

    @Resource
    private IUserService userService;

    @PostConstruct
    public void setUserService() {
        staticUserService = userService;
    }


    public static String genToken(String userId, String sign) {
        return JWT.create().withAudience(userId) // 将 user id 保存到 token 里面,作为载荷
                .withExpiresAt(DateUtil.offsetHour(new Date(), 12)) // 12 小时后 token 过期
                .sign(Algorithm.HMAC256(sign)); // 以 password 作为 token 的密钥
    }

    public static User getCurrentUser() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String token = request.getHeader("token");
            if (StrUtil.isNotBlank(token)) {
                String userId = JWT.decode(token).getAudience().get(0);
                return staticUserService.getById(Integer.valueOf(userId));
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    // 用于 @AuthAccess 接口：不验签的 getCurrentUser 可能被伪造 token 冒充管理员
    public static User getVerifiedCurrentUser() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String token = request.getHeader("token");
            if (StrUtil.isBlank(token)) {
                return null;
            }
            String userId = JWT.decode(token).getAudience().get(0);
            User user = staticUserService.getById(Integer.valueOf(userId));
            if (user == null) {
                return null;
            }
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(user.getPassword())).build();
            verifier.verify(token);
            return user;
        } catch (Exception e) {
            return null;
        }
    }

}
