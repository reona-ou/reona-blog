package com.reona.blog.service;

import cn.hutool.core.util.StrUtil;
import com.reona.blog.entity.AssistantSetting;
import com.reona.blog.mapper.AssistantSettingMapper;
import com.reona.blog.utils.AssistantPromptBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class AssistantSettingService {

    private static final String DEFAULT_ASSISTANT_NAME = "Default Assistant";

    @Resource
    private AssistantSettingMapper assistantSettingMapper;

    @Value("${chat.api.url:http://127.0.0.1:8080/v1/chat/completions}")
    private String defaultApiUrl;

    @Value("${chat.api.model:qwen3.5-4b}")
    private String defaultModel;

    public AssistantSetting get() {
        AssistantSetting setting = assistantSettingMapper.selectById(1);
        if (setting != null) {
            return setting;
        }
        AssistantSetting created = new AssistantSetting();
        created.setId(1);
        created.setAssistantName(DEFAULT_ASSISTANT_NAME);
        created.setApiUrl(defaultApiUrl);
        created.setApiKey("");
        created.setModel(defaultModel);
        created.setTemperature(0.6);
        created.setMaxTokens(420);
        created.setEnabled(true);
        try {
            assistantSettingMapper.insert(created);
            return created;
        } catch (DuplicateKeyException e) {
            AssistantSetting existing = assistantSettingMapper.selectById(1);
            return existing == null ? created : existing;
        }
    }

    public void update(AssistantSetting setting) {
        setting.setId(1);
        assistantSettingMapper.updateById(setting);
    }

    public String name(AssistantSetting setting) {
        return StrUtil.isBlank(setting.getAssistantName()) ? DEFAULT_ASSISTANT_NAME : setting.getAssistantName().trim();
    }

    public String apiUrl(AssistantSetting setting) {
        return StrUtil.isBlank(setting.getApiUrl()) ? defaultApiUrl : setting.getApiUrl().trim();
    }

    public String model(AssistantSetting setting) {
        return StrUtil.isBlank(setting.getModel()) ? defaultModel : setting.getModel().trim();
    }

    public double temperature(AssistantSetting setting) {
        return setting.getTemperature() == null ? 0.6 : setting.getTemperature();
    }

    public int maxTokens(AssistantSetting setting) {
        return setting.getMaxTokens() == null ? 420 : setting.getMaxTokens();
    }

    public boolean enabled(AssistantSetting setting) {
        return !Boolean.FALSE.equals(setting.getEnabled());
    }

    public String defaultPersonaPrompt() {
        return AssistantPromptBuilder.DEFAULT_PERSONA_PROMPT;
    }

    public String voice(AssistantSetting setting, String language) {
        String stored;
        if ("en".equalsIgnoreCase(language)) {
            stored = setting.getVoiceEn();
        } else if ("ja".equalsIgnoreCase(language)) {
            stored = setting.getVoiceJa();
        } else {
            stored = setting.getVoiceZh();
        }
        return StrUtil.isBlank(stored)
                ? AssistantPromptBuilder.languageVoice(language)
                : stored.trim() + "\n" + AssistantPromptBuilder.languageAnchor(language);
    }

    public String example(AssistantSetting setting, String language) {
        String stored;
        if ("en".equalsIgnoreCase(language)) {
            stored = setting.getExampleEn();
        } else if ("ja".equalsIgnoreCase(language)) {
            stored = setting.getExampleJa();
        } else {
            stored = setting.getExampleZh();
        }
        String anchor = AssistantPromptBuilder.languageExample(language);
        if (StrUtil.isBlank(stored)) {
            return anchor;
        }
        return "zh".equalsIgnoreCase(language)
                ? stored.trim() + "\n" + anchor
                : anchor + "\n" + stored.trim();
    }

    public String defaultVoice(String language) {
        return AssistantPromptBuilder.languageVoice(language);
    }

    public String defaultExample(String language) {
        return AssistantPromptBuilder.languageExample(language);
    }
}
