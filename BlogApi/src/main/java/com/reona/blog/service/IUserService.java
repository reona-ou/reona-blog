package com.reona.blog.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.reona.blog.commcn.Result;
import com.reona.blog.controller.dto.UserDTO;
import com.reona.blog.controller.dto.UserPasswordDTO;
import com.reona.blog.entity.User;

public interface IUserService extends IService<User> {

    UserDTO login(UserDTO userDTO);

    Result register(UserDTO userDTO);

    Result getUserByName(String username);

    Integer updatePassword(UserPasswordDTO userPasswordDTO);

    void resetPassword(Integer id, String newPassword);
}
