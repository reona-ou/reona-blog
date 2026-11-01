package com.reona.blog.entity;

import cn.hutool.core.annotation.Alias;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.io.Serializable;

import java.time.OffsetDateTime;


@Getter
@Setter
@NoArgsConstructor // 无参构造
@AllArgsConstructor // 有参构造
@TableName("sys_user")
@ToString(exclude = "password")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Alias("id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Alias("用户名")
    private String username;

    @JsonIgnore
    @Alias("密码")
    private String password;

    @Alias("昵称")
    private String nickname;

    @Alias("邮箱")
    private String email;

    @Alias("电话")
    private String phone;

    @Alias("地址")
    private String address;

    @Alias("创建时间")
    private OffsetDateTime createTime;

    @Alias("头像")
    private String avatarUrl;

    @Alias("角色")
    private String role;
}
