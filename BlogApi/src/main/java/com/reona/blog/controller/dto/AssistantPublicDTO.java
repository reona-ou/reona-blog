package com.reona.blog.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssistantPublicDTO {
    private String name;
    private Boolean enabled;
}
