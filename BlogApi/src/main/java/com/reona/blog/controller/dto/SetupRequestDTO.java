package com.reona.blog.controller.dto;

import lombok.Data;

@Data
public class SetupRequestDTO {

    private String siteName;
    private String siteEmail;
    private String ownerUsername;
    private String ownerEmail;
    private String ownerNickname;
    private String ownerPassword;
    private Boolean configureAssistant;
    private Boolean assistantEnabled;
    private String assistantApiUrl;
    private String assistantModel;
    private String assistantApiKey;
    private Boolean createSampleContent;
}
