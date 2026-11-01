package com.reona.blog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.reona.blog.commcn.Constants;
import com.reona.blog.commcn.Result;
import com.reona.blog.commcn.RoleEnum;
import com.reona.blog.controller.dto.UserDTO;
import com.reona.blog.controller.dto.UserPasswordDTO;
import com.reona.blog.entity.User;
import com.reona.blog.exception.ServiceException;
import com.reona.blog.mapper.UserMapper;
import com.reona.blog.service.IUserService;
import com.reona.blog.utils.PasswordUtils;
import com.reona.blog.utils.TokenUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private static final Log LOG = Log.get();

    @Override
    public UserDTO login(UserDTO userDTO) {
        String account = userDTO.getUsername() == null ? "" : userDTO.getUsername().trim();
        String rawPassword = userDTO.getPassword();
        try {
            User user = getOne(new QueryWrapper<User>().eq("email", account), false);
            if (user == null) {
                user = getOne(new QueryWrapper<User>().eq("username", account), false);
            }
            if (user == null || !PasswordUtils.matches(rawPassword, user.getPassword())) {
                throw new ServiceException(Constants.CODE_600, "邮箱或密码错误");
            }
            if (!PasswordUtils.isHashed(user.getPassword())) {
                user.setPassword(PasswordUtils.hash(rawPassword));
                boolean migrated = update(new UpdateWrapper<User>()
                        .eq("id", user.getId())
                        .set("password", user.getPassword()));
                if (!migrated) {
                    throw new ServiceException(Constants.CODE_500, "系统错误");
                }
            }
            BeanUtil.copyProperties(user, userDTO, true);
            userDTO.setToken(TokenUtils.genToken(user.getId().toString(), user.getPassword()));
            userDTO.setPassword(null);
            return userDTO;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e);
            throw new ServiceException(Constants.CODE_500, "系统错误");
        }
    }

    @Override
    public Result register(UserDTO userDTO) {
        PasswordUtils.validate(userDTO.getPassword());
        String username = userDTO.getUsername() == null ? "" : userDTO.getUsername().trim();
        if (username.isEmpty() || username.length() > 32) {
            throw new ServiceException(Constants.CODE_400, "用户名长度需要在 1–32 个字符之间");
        }
        if (username.contains("@")) {
            throw new ServiceException(Constants.CODE_400, "用户名不能包含 @");
        }
        String nickname = userDTO.getNickname() == null ? "" : userDTO.getNickname().trim();
        if (nickname.isEmpty()) {
            nickname = username;
        }
        if (nickname.length() > 32) {
            throw new ServiceException(Constants.CODE_400, "昵称长度不能超过 32 个字符");
        }
        User existingUser = getOne(new QueryWrapper<User>().eq("username", username), false);
        if (existingUser != null) {
            throw new ServiceException(Constants.CODE_600, "该用户名已存在");
        }
        String email = userDTO.getEmail() == null ? "" : userDTO.getEmail().trim();
        if (email.isEmpty() || email.length() > 128
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ServiceException(Constants.CODE_400, "请填写有效的邮箱");
        }
        User existingEmail = getOne(new QueryWrapper<User>().eq("email", email), false);
        if (existingEmail != null) {
            throw new ServiceException(Constants.CODE_600, "该邮箱已被注册");
        }

        User user = new User();
        BeanUtil.copyProperties(userDTO, user, true);
        user.setId(null);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPassword(PasswordUtils.hash(userDTO.getPassword()));
        user.setRole(RoleEnum.ROLE_USER.name());
        user.setAvatarUrl("/file/头像.jpg");
        try {
            if (!save(user)) {
                throw new ServiceException(Constants.CODE_500, "系统错误");
            }
        } catch (DuplicateKeyException e) {
            if (getOne(new QueryWrapper<User>().eq("username", username), false) != null) {
                throw new ServiceException(Constants.CODE_600, "该用户名已存在");
            }
            if (getOne(new QueryWrapper<User>().eq("email", email), false) != null) {
                throw new ServiceException(Constants.CODE_600, "该邮箱已被注册");
            }
            throw new ServiceException(Constants.CODE_600, "该用户名或邮箱已被使用");
        }
        return Result.success();
    }

    @Override
    public Result getUserByName(String username) {
        User user = getOne(new QueryWrapper<User>().eq("username", username), false);
        return Result.success(user);
    }

    @Override
    public Integer updatePassword(UserPasswordDTO userPasswordDTO) {
        PasswordUtils.validate(userPasswordDTO.getNewPassword());
        User user = getOne(new QueryWrapper<User>().eq("username", userPasswordDTO.getUsername()), false);
        if (user == null || !PasswordUtils.matches(userPasswordDTO.getPassword(), user.getPassword())) {
            throw new ServiceException(Constants.CODE_600, "原密码错误");
        }
        String hashed = PasswordUtils.hash(userPasswordDTO.getNewPassword());
        return update(new UpdateWrapper<User>()
                .eq("id", user.getId())
                .set("password", hashed)) ? 1 : 0;
    }

    @Override
    public void resetPassword(Integer id, String newPassword) {
        PasswordUtils.validate(newPassword);
        User user = getById(id);
        if (user == null) {
            throw new ServiceException(Constants.CODE_404, "用户不存在");
        }
        String hashed = PasswordUtils.hash(newPassword);
        if (!update(new UpdateWrapper<User>().eq("id", id).set("password", hashed))) {
            throw new ServiceException(Constants.CODE_500, "密码重置失败");
        }
    }
}
