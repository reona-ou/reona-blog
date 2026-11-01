package com.reona.blog.controller;

import com.reona.blog.commcn.Constants;
import com.reona.blog.commcn.Result;
import com.reona.blog.commcn.RoleEnum;
import com.reona.blog.config.AuthAccess;
import com.reona.blog.controller.dto.SiteSettingDTO;
import com.reona.blog.entity.SiteSetting;
import com.reona.blog.entity.User;
import com.reona.blog.mapper.SiteSettingMapper;
import com.reona.blog.utils.TokenUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/settings")
public class SiteSettingController {

    @Resource
    private SiteSettingMapper siteSettingMapper;

    @AuthAccess
    @GetMapping
    public Result get() {
        return Result.success(toDto(loadOrCreate()));
    }

    @PutMapping
    public Result update(@RequestBody SiteSettingDTO request) {
        if (!isSuperAdmin()) {
            return Result.error(Constants.CODE_403, "只有超级管理员可以修改站点设置");
        }
        if (request == null) {
            return Result.error(Constants.CODE_400, "参数错误");
        }

        String siteName = request.getSiteName() == null ? "" : request.getSiteName().trim();
        if (siteName.isEmpty() || siteName.length() > 32) {
            return Result.error(Constants.CODE_400, "站点名称长度需要在 1–32 个字符之间");
        }

        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        if (email.length() > 128 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return Result.error(Constants.CODE_400, "请填写有效的联系邮箱");
        }

        SiteSetting setting = loadOrCreate();
        setting.setSiteName(siteName);
        setting.setEmail(email);
        siteSettingMapper.updateById(setting);
        return Result.success(toDto(setting));
    }

    private SiteSetting loadOrCreate() {
        SiteSetting setting = siteSettingMapper.selectById(1);
        if (setting != null) {
            return setting;
        }
        SiteSetting created = new SiteSetting();
        created.setId(1);
        created.setSiteName("Default Blog");
        created.setEmail("default@example.com");
        try {
            siteSettingMapper.insert(created);
            return created;
        } catch (DuplicateKeyException e) {
            SiteSetting existing = siteSettingMapper.selectById(1);
            return existing == null ? created : existing;
        }
    }

    private SiteSettingDTO toDto(SiteSetting setting) {
        SiteSettingDTO dto = new SiteSettingDTO();
        dto.setSiteName(setting.getSiteName());
        dto.setEmail(setting.getEmail());
        return dto;
    }

    private boolean isSuperAdmin() {
        User user = TokenUtils.getVerifiedCurrentUser();
        return user != null && RoleEnum.isSuperAdmin(user.getRole());
    }
}
