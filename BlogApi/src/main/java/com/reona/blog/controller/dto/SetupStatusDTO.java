package com.reona.blog.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SetupStatusDTO {

    private boolean required;
    private String siteName;
    private String email;
}
