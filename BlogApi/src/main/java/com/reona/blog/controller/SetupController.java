package com.reona.blog.controller;

import com.reona.blog.commcn.Constants;
import com.reona.blog.commcn.Result;
import com.reona.blog.config.AuthAccess;
import com.reona.blog.controller.dto.SetupRequestDTO;
import com.reona.blog.service.RateLimiter;
import com.reona.blog.service.SetupService;
import com.reona.blog.utils.ClientIpUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/setup")
public class SetupController {

    private static final int SETUP_MAX_REQUESTS = 5;
    private static final long SETUP_WINDOW_MS = 10 * 60 * 1000L;

    @Resource
    private SetupService setupService;

    @Resource
    private RateLimiter rateLimiter;

    @Value("${proxy.trusted:127.0.0.1,0:0:0:0:0:0:0:1,::1}")
    private String trustedProxies;

    @AuthAccess
    @GetMapping("/status")
    public Result status() {
        return Result.success(setupService.status());
    }

    @AuthAccess
    @PostMapping("/initialize")
    public Result initialize(@RequestBody SetupRequestDTO request, HttpServletRequest httpRequest) {
        String ip = ClientIpUtils.clientIp(httpRequest, trustedProxies);
        if (!ClientIpUtils.isPrivateOrLoopback(ip)) {
            return Result.error(Constants.CODE_403, "初始化只能在本机或局域网完成");
        }
        if (!rateLimiter.tryAcquire("setup|" + ip, SETUP_MAX_REQUESTS, SETUP_WINDOW_MS)) {
            return Result.error(Constants.CODE_429, "尝试次数过多，请稍后再试");
        }
        return Result.success(setupService.initialize(request));
    }
}
