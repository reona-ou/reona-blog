package com.reona.blog.controller.dto;

import lombok.Data;

@Data
public class UserAdminUpdateDTO {
    private String nickname;
    private String email;
    private String phone;
    private String address;
}
