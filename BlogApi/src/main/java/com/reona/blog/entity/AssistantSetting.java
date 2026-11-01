package com.reona.blog.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("sys_assistant_setting")
public class AssistantSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String assistantName;

    private String apiUrl;

    private String apiKey;

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

    private Boolean enabled;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
