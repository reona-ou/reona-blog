package com.reona.blog.controller.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ArticleSummaryDTO {

    private Integer id;
    private String label;
    private String title;
    private String summary;
    private Boolean enable;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;
}
