package com.reona.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Getter
@Setter
@TableName("sys_comment")
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer articleId;

    private Integer parentId;

    private Integer userId;

    private String username;

    private String nickname;

    private String content;

    private Boolean isDelete;

    private OffsetDateTime createTime;

    @TableField(exist = false)
    private Long likeCount;

    @TableField(exist = false)
    private Boolean liked;
}
