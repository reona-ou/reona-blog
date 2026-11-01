package com.reona.blog.controller;

import cn.hutool.core.util.StrUtil;
import com.reona.blog.commcn.Constants;
import com.reona.blog.commcn.Result;
import com.reona.blog.commcn.RoleEnum;
import com.reona.blog.config.AuthAccess;
import com.reona.blog.controller.dto.AssistantPublicDTO;
import com.reona.blog.controller.dto.AssistantSettingsDTO;
import com.reona.blog.entity.AssistantSetting;
import com.reona.blog.entity.User;
import com.reona.blog.service.AssistantSettingService;
import com.reona.blog.utils.AssistantUrlValidator;
import com.reona.blog.utils.TokenUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/assistant")
public class AssistantController {

    @Resource
    private AssistantSettingService assistantSettingService;

    @AuthAccess
    @GetMapping("/public")
    public Result publicInfo() {
        AssistantSetting setting = assistantSettingService.get();
        return Result.success(new AssistantPublicDTO(
                assistantSettingService.name(setting),
                assistantSettingService.enabled(setting)
        ));
    }

    @GetMapping("/settings")
    public Result settings() {
        if (!isSuperAdmin()) {
            return Result.error(Constants.CODE_403, "只有超级管理员可以查看助手设置");
        }
        return Result.success(toDto(assistantSettingService.get()));
    }

    @PutMapping("/settings")
    public Result update(@RequestBody AssistantSettingsDTO request) {
        if (!isSuperAdmin()) {
            return Result.error(Constants.CODE_403, "只有超级管理员可以修改助手设置");
        }
        if (request == null) {
            return Result.error(Constants.CODE_400, "参数错误");
        }

        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty() || name.length() > 32) {
            return Result.error(Constants.CODE_400, "助手名称长度需要在 1–32 个字符之间");
        }
        String apiUrl = request.getApiUrl() == null ? "" : request.getApiUrl().trim();
        if (!AssistantUrlValidator.isValid(apiUrl)) {
            return Result.error(Constants.CODE_400, "请填写有效的 HTTP 接口地址");
        }
        if (AssistantUrlValidator.isBlocked(apiUrl)) {
            return Result.error(Constants.CODE_400, "接口地址不允许指向云元数据或链路本地地址");
        }
        String model = request.getModel() == null ? "" : request.getModel().trim();
        if (model.isEmpty() || model.length() > 128) {
            return Result.error(Constants.CODE_400, "请填写模型名称（不超过 128 个字符）");
        }
        double temperature = request.getTemperature() == null ? 0.6 : request.getTemperature();
        if (temperature < 0 || temperature > 2) {
            return Result.error(Constants.CODE_400, "温度需要在 0 到 2 之间");
        }
        int maxTokens = request.getMaxTokens() == null ? 420 : request.getMaxTokens();
        if (maxTokens < 32 || maxTokens > 4096) {
            return Result.error(Constants.CODE_400, "最大 token 需要在 32 到 4096 之间");
        }
        String personaPrompt = request.getPersonaPrompt() == null ? "" : request.getPersonaPrompt().trim();
        if (personaPrompt.length() > 20000) {
            return Result.error(Constants.CODE_400, "Prompt 不能超过 20000 个字符");
        }
        String apiKey = request.getApiKey() == null ? "" : request.getApiKey().trim();
        if (apiKey.length() > 255) {
            return Result.error(Constants.CODE_400, "API Key 不能超过 255 个字符");
        }
        String voiceZh = request.getVoiceZh() == null ? "" : request.getVoiceZh().trim();
        String voiceEn = request.getVoiceEn() == null ? "" : request.getVoiceEn().trim();
        String voiceJa = request.getVoiceJa() == null ? "" : request.getVoiceJa().trim();
        String exampleZh = request.getExampleZh() == null ? "" : request.getExampleZh().trim();
        String exampleEn = request.getExampleEn() == null ? "" : request.getExampleEn().trim();
        String exampleJa = request.getExampleJa() == null ? "" : request.getExampleJa().trim();
        if (voiceZh.length() > 4000 || voiceEn.length() > 4000 || voiceJa.length() > 4000
                || exampleZh.length() > 4000 || exampleEn.length() > 4000 || exampleJa.length() > 4000) {
            return Result.error(Constants.CODE_400, "语言语气和示例不能超过 4000 个字符");
        }

        AssistantSetting setting = assistantSettingService.get();
        setting.setAssistantName(name);
        setting.setApiUrl(apiUrl);
        setting.setModel(model);
        setting.setTemperature(temperature);
        setting.setMaxTokens(maxTokens);
        setting.setPersonaPrompt(personaPrompt);
        setting.setVoiceZh(voiceZh);
        setting.setVoiceEn(voiceEn);
        setting.setVoiceJa(voiceJa);
        setting.setExampleZh(exampleZh);
        setting.setExampleEn(exampleEn);
        setting.setExampleJa(exampleJa);
        setting.setEnabled(request.getEnabled() == null || request.getEnabled());
        if (Boolean.TRUE.equals(request.getClearApiKey())) {
            setting.setApiKey("");
        } else if (!apiKey.isEmpty()) {
            setting.setApiKey(apiKey);
        }
        assistantSettingService.update(setting);
        return Result.success(toDto(assistantSettingService.get()));
    }

    private AssistantSettingsDTO toDto(AssistantSetting setting) {
        AssistantSettingsDTO dto = new AssistantSettingsDTO();
        dto.setName(assistantSettingService.name(setting));
        dto.setApiUrl(assistantSettingService.apiUrl(setting));
        dto.setModel(assistantSettingService.model(setting));
        dto.setTemperature(assistantSettingService.temperature(setting));
        dto.setMaxTokens(assistantSettingService.maxTokens(setting));
        dto.setEnabled(assistantSettingService.enabled(setting));
        dto.setHasApiKey(StrUtil.isNotBlank(setting.getApiKey()));
        dto.setUsingDefaultPrompt(StrUtil.isBlank(setting.getPersonaPrompt()));
        dto.setPersonaPrompt(StrUtil.isBlank(setting.getPersonaPrompt())
                ? assistantSettingService.defaultPersonaPrompt() : setting.getPersonaPrompt());
        dto.setDefaultPersonaPrompt(assistantSettingService.defaultPersonaPrompt());
        dto.setVoiceZh(setting.getVoiceZh());
        dto.setVoiceEn(setting.getVoiceEn());
        dto.setVoiceJa(setting.getVoiceJa());
        dto.setExampleZh(setting.getExampleZh());
        dto.setExampleEn(setting.getExampleEn());
        dto.setExampleJa(setting.getExampleJa());
        dto.setDefaultVoiceZh(assistantSettingService.defaultVoice("zh"));
        dto.setDefaultVoiceEn(assistantSettingService.defaultVoice("en"));
        dto.setDefaultVoiceJa(assistantSettingService.defaultVoice("ja"));
        dto.setDefaultExampleZh(assistantSettingService.defaultExample("zh"));
        dto.setDefaultExampleEn(assistantSettingService.defaultExample("en"));
        dto.setDefaultExampleJa(assistantSettingService.defaultExample("ja"));
        return dto;
    }

    private boolean isSuperAdmin() {
        User user = TokenUtils.getVerifiedCurrentUser();
        return user != null && RoleEnum.isSuperAdmin(user.getRole());
    }
}
