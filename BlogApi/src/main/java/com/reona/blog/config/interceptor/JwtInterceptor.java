package com.reona.blog.config.interceptor;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.reona.blog.commcn.Constants;
import com.reona.blog.config.AuthAccess;
import com.reona.blog.entity.User;
import com.reona.blog.exception.ServiceException;
import com.reona.blog.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

public class JwtInterceptor implements HandlerInterceptor {
    @Autowired
    private IUserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("token");
        // 如果不是映射到方法直接通过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        } else {
            //自定义注解
            HandlerMethod h = (HandlerMethod) handler;
            AuthAccess authAccess = h.getMethodAnnotation(AuthAccess.class);
            if (authAccess != null) {
                return true;
            }
        }
        if (StrUtil.isBlank(token)) {
            throw new ServiceException(Constants.CODE_401, "无token，请重新登录");
        }
        // 获取 token 中的 user id
        //验证token
        List<String> audience;
        try {
            audience = JWT.decode(token).getAudience();
        } catch (JWTDecodeException j) {
            throw new ServiceException(Constants.CODE_401, "token验证失败，请重新登录");
        }
        if (audience == null || audience.isEmpty() || StrUtil.isBlank(audience.get(0))) {
            throw new ServiceException(Constants.CODE_401, "token验证失败，请重新登录");
        }
        Integer userId;
        try {
            userId = Integer.valueOf(audience.get(0));
        } catch (NumberFormatException e) {
            throw new ServiceException(Constants.CODE_401, "token验证失败，请重新登录");
        }
        //根据token的userid查询数据库
        User user = userService.getById(userId);
        if (user == null) {
            throw new ServiceException(Constants.CODE_401, "用户不存在，请重新登录");
        }
        // 用户密码加签验证 token
        if (StrUtil.isBlank(user.getPassword())) {
            throw new ServiceException(Constants.CODE_401, "token验证失败，请重新登录");
        }
        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(user.getPassword())).build();
        try {
            jwtVerifier.verify(token); // 验证token
        } catch (JWTVerificationException e) {
            throw new ServiceException(Constants.CODE_401, "token验证失败，请重新登录");
        }
        return true;
    }
}
