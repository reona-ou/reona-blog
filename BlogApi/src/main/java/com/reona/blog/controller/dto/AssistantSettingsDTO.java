package com.reona.blog.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssistantSettingsDTO {
    private String name;
    private String apiUrl;
    private String apiKey;
    private Boolean clearApiKey;
    private Boolean hasApiKey;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private String personaPrompt;
    private String voiceZh;
    private String voiceEn;
    private String voiceJa;
    private String exampleZh;
    private String exampleEn;
    private String exampleJa;
    private String defaultVoiceZh;
    private String defaultVoiceEn;
    private String defaultVoiceJa;
    private String defaultExampleZh;
    private String defaultExampleEn;
    private String defaultExampleJa;
    private Boolean enabled;
    private String defaultPersonaPrompt;
    private Boolean usingDefaultPrompt;
}
