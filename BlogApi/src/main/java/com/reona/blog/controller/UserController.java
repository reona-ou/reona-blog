package com.reona.blog.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reona.blog.commcn.Constants;
import com.reona.blog.commcn.Result;
import com.reona.blog.commcn.RoleEnum;
import com.reona.blog.controller.dto.UserAdminUpdateDTO;
import com.reona.blog.controller.dto.UserDTO;
import com.reona.blog.controller.dto.UserPasswordDTO;
import com.reona.blog.controller.dto.UserResetPasswordDTO;
import com.reona.blog.controller.dto.UserRoleDTO;
import com.reona.blog.entity.ArticleLike;
import com.reona.blog.entity.CommentLike;
import com.reona.blog.entity.User;
import com.reona.blog.exception.ServiceException;
import com.reona.blog.mapper.ArticleLikeMapper;
import com.reona.blog.mapper.CommentLikeMapper;
import com.reona.blog.service.IUserService;
import com.reona.blog.service.RateLimiter;
import com.reona.blog.utils.ClientIpUtils;
import com.reona.blog.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private ArticleLikeMapper articleLikeMapper;

    @Resource
    private CommentLikeMapper commentLikeMapper;

    @Resource
    private RateLimiter rateLimiter;

    @Value("${proxy.trusted:127.0.0.1,0:0:0:0:0:0:0:1,::1}")
    private String trustedProxies;

    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final long LOGIN_WINDOW_MS = 10 * 60 * 1000L;
    private static final int REGISTER_MAX_REQUESTS = 5;
    private static final long REGISTER_WINDOW_MS = 10 * 60 * 1000L;

    @PostMapping("/login")
    public Result login(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        String username = userDTO.getUsername();
        String password = userDTO.getPassword();
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return Result.error(Constants.CODE_400, "参数错误");
        }
        String key = ClientIpUtils.clientIp(request, trustedProxies)
                + "|" + username.trim().toLowerCase(Locale.ROOT);
        if (rateLimiter.isBlocked(key, LOGIN_MAX_ATTEMPTS, LOGIN_WINDOW_MS)) {
            return Result.error(Constants.CODE_429, "尝试次数过多，请 10 分钟后再试");
        }
        try {
            UserDTO data = userService.login(userDTO);
            rateLimiter.reset(key);
            return Result.success(data);
        } catch (ServiceException e) {
            if (Constants.CODE_600.equals(e.getCode())) {
                rateLimiter.recordFailure(key, LOGIN_WINDOW_MS);
            }
            throw e;
        }
    }

    @PostMapping("/register")
    public Result register(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        String username = userDTO == null ? null : userDTO.getUsername();
        String password = userDTO == null ? null : userDTO.getPassword();
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return Result.error(Constants.CODE_400, "参数错误");
        }
        String ip = ClientIpUtils.clientIp(request, trustedProxies);
        if (!rateLimiter.tryAcquire("register|" + ip, REGISTER_MAX_REQUESTS, REGISTER_WINDOW_MS)) {
            return Result.error(Constants.CODE_429, "注册过于频繁，请稍后再试");
        }
        return userService.register(userDTO);
    }

    @PostMapping("/password")
    public Result password(@RequestBody UserPasswordDTO userPasswordDTO) {
        User current = TokenUtils.getVerifiedCurrentUser();
        if (current == null) {
            return Result.error(Constants.CODE_401, "请先登录");
        }
        if (userPasswordDTO == null || StrUtil.isBlank(userPasswordDTO.getUsername())) {
            return Result.error(Constants.CODE_400, "参数错误");
        }
        if (!current.getUsername().equals(userPasswordDTO.getUsername())) {
            return Result.error(Constants.CODE_403, "只能修改自己的密码");
        }
        Integer updated = userService.updatePassword(userPasswordDTO);
        if (updated == null || updated == 0) {
            return Result.error(Constants.CODE_500, "密码修改失败");
        }
        return Result.success();
    }

    @PutMapping("/profile")
    public Result updateProfile(@RequestBody UserAdminUpdateDTO request) {
        User current = TokenUtils.getVerifiedCurrentUser();
        if (current == null) {
            return Result.error(Constants.CODE_401, "请先登录");
        }
        ProfileData profile = validateProfile(request);
        ensureEmailAvailable(profile.email(), current.getId());
        User patch = new User();
        patch.setId(current.getId());
        patch.setNickname(profile.nickname());
        patch.setEmail(profile.email());
        patch.setPhone(profile.phone());
        patch.setAddress(profile.address());
        if (!userService.updateById(patch)) {
            return Result.error(Constants.CODE_500, "资料保存失败");
        }
        current.setNickname(profile.nickname());
        current.setEmail(profile.email());
        current.setPhone(profile.phone());
        current.setAddress(profile.address());
        return Result.success(current);
    }

    @GetMapping("/manage/page")
    public Result managePage(@RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             @RequestParam(defaultValue = "") String keyword,
                             @RequestParam(defaultValue = "") String role) {
        User current = TokenUtils.getCurrentUser();
        if (!RoleEnum.isAdmin(current == null ? null : current.getRole())) {
            return Result.error(Constants.CODE_403, "无权限查看用户");
        }

        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 50);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (RoleEnum.isSuperAdmin(current.getRole())) {
            if (StrUtil.isNotBlank(role)) {
                if (!RoleEnum.ROLE_USER.name().equals(role)
                        && !RoleEnum.ROLE_ADMIN.name().equals(role)
                        && !RoleEnum.ROLE_SUPER_ADMIN.name().equals(role)) {
                    return Result.error(Constants.CODE_400, "角色参数不正确");
                }
                queryWrapper.eq("role", role);
            }
        } else {
            queryWrapper.eq("role", RoleEnum.ROLE_USER.name());
        }

        String search = keyword == null ? "" : keyword.trim();
        if (!search.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper.like("username", search)
                    .or().like("nickname", search)
                    .or().like("email", search));
        }
        queryWrapper.orderByDesc("id");
        return Result.success(userService.page(new Page<>(safePageNum, safePageSize), queryWrapper));
    }

    @PutMapping("/manage/{id}")
    public Result updateManagedUser(@PathVariable Integer id, @RequestBody UserAdminUpdateDTO request) {
        User current = TokenUtils.getCurrentUser();
        if (!RoleEnum.isAdmin(current == null ? null : current.getRole())) {
            return Result.error(Constants.CODE_403, "无权限管理用户");
        }
        User target = userService.getById(id);
        if (target == null) {
            return Result.error(Constants.CODE_404, "用户不存在");
        }
        if (!canManageUser(current, target)) {
            return Result.error(Constants.CODE_403, "无权限管理该用户");
        }
        ProfileData profile = validateProfile(request);
        ensureEmailAvailable(profile.email(), id);

        User patch = new User();
        patch.setId(id);
        patch.setNickname(profile.nickname());
        patch.setEmail(profile.email());
        patch.setPhone(profile.phone());
        patch.setAddress(profile.address());
        if (!userService.updateById(patch)) {
            return Result.error(Constants.CODE_500, "资料保存失败");
        }
        target.setNickname(profile.nickname());
        target.setEmail(profile.email());
        target.setPhone(profile.phone());
        target.setAddress(profile.address());
        return Result.success(target);
    }

    @PutMapping("/manage/{id}/password")
    public Result resetManagedUserPassword(@PathVariable Integer id, @RequestBody UserResetPasswordDTO request) {
        User current = TokenUtils.getCurrentUser();
        if (!RoleEnum.isAdmin(current == null ? null : current.getRole())) {
            return Result.error(Constants.CODE_403, "无权限管理用户");
        }
        User target = userService.getById(id);
        if (target == null) {
            return Result.error(Constants.CODE_404, "用户不存在");
        }
        if (!canManageUser(current, target)) {
            return Result.error(Constants.CODE_403, "无权限管理该用户");
        }
        String newPassword = request == null ? null : request.getNewPassword();
        userService.resetPassword(id, newPassword);
        return Result.success();
    }

    @PutMapping("/manage/{id}/role")
    public Result updateManagedUserRole(@PathVariable Integer id, @RequestBody UserRoleDTO request) {
        User current = TokenUtils.getCurrentUser();
        if (!RoleEnum.isSuperAdmin(current == null ? null : current.getRole())) {
            return Result.error(Constants.CODE_403, "只有超级管理员可以调整管理员权限");
        }
        if (current.getId().equals(id)) {
            return Result.error(Constants.CODE_400, "不能修改自己的角色");
        }
        User target = userService.getById(id);
        if (target == null) {
            return Result.error(Constants.CODE_404, "用户不存在");
        }
        if (RoleEnum.isSuperAdmin(target.getRole())) {
            return Result.error(Constants.CODE_400, "不能修改超级管理员");
        }
        String role = request == null || request.getRole() == null ? "" : request.getRole().trim();
        if (!RoleEnum.ROLE_ADMIN.name().equals(role) && !RoleEnum.ROLE_USER.name().equals(role)) {
            return Result.error(Constants.CODE_400, "角色参数不正确");
        }
        User patch = new User();
        patch.setId(id);
        patch.setRole(role);
        if (!userService.updateById(patch)) {
            return Result.error(Constants.CODE_500, "角色修改失败");
        }
        target.setRole(role);
        return Result.success(target);
    }

    @DeleteMapping("/manage/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result deleteManagedUser(@PathVariable Integer id) {
        User current = TokenUtils.getCurrentUser();
        if (!RoleEnum.isAdmin(current == null ? null : current.getRole())) {
            return Result.error(Constants.CODE_403, "无权限删除用户");
        }
        if (current.getId().equals(id)) {
            return Result.error(Constants.CODE_400, "不能删除自己的账号");
        }
        User target = userService.getById(id);
        if (target == null) {
            return Result.error(Constants.CODE_404, "用户不存在");
        }
        if (RoleEnum.isSuperAdmin(target.getRole())) {
            return Result.error(Constants.CODE_400, "不能删除超级管理员");
        }
        if (!canManageUser(current, target)) {
            return Result.error(Constants.CODE_403, "无权限删除该用户");
        }

        userService.removeById(id);
        articleLikeMapper.delete(new QueryWrapper<ArticleLike>().eq("user_id", id));
        commentLikeMapper.delete(new QueryWrapper<CommentLike>().eq("user_id", id));
        return Result.success();
    }

    @GetMapping("/personify/{username}")
    public Result findUserByName(@PathVariable String username) {
        User current = TokenUtils.getCurrentUser();
        if (current == null) {
            return Result.error(Constants.CODE_401, "请先登录");
        }
        User target = userService.getOne(new QueryWrapper<User>().eq("username", username));
        if (target == null) {
            return Result.error(Constants.CODE_404, "用户不存在");
        }
        if (!canReadUser(current, target)) {
            return Result.error(Constants.CODE_403, "无权限查看该用户");
        }
        return Result.success(target);
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        User current = TokenUtils.getCurrentUser();
        if (current == null) {
            return Result.error(Constants.CODE_401, "请先登录");
        }
        User target = userService.getById(id);
        if (target == null) {
            return Result.error(Constants.CODE_404, "用户不存在");
        }
        if (!canReadUser(current, target)) {
            return Result.error(Constants.CODE_403, "无权限查看该用户");
        }
        return Result.success(target);
    }

    private ProfileData validateProfile(UserAdminUpdateDTO request) {
        if (request == null) {
            throw new ServiceException(Constants.CODE_400, "参数错误");
        }
        String nickname = request.getNickname() == null ? "" : request.getNickname().trim();
        if (nickname.isEmpty() || nickname.length() > 32) {
            throw new ServiceException(Constants.CODE_400, "昵称长度需要在 1–32 个字符之间");
        }
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        if (email.isEmpty() || email.length() > 128 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ServiceException(Constants.CODE_400, "请填写有效的邮箱");
        }
        String phone = request.getPhone() == null ? "" : request.getPhone().trim();
        if (phone.length() > 11) {
            throw new ServiceException(Constants.CODE_400, "电话长度不能超过 11 个字符");
        }
        String address = request.getAddress() == null ? "" : request.getAddress().trim();
        if (address.length() > 255) {
            throw new ServiceException(Constants.CODE_400, "地址长度不能超过 255 个字符");
        }
        return new ProfileData(nickname, email, phone, address);
    }

    private void ensureEmailAvailable(String email, Integer excludeId) {
        QueryWrapper<User> emailQuery = new QueryWrapper<>();
        emailQuery.eq("email", email).ne("id", excludeId);
        if (userService.count(emailQuery) > 0) {
            throw new ServiceException(Constants.CODE_600, "该邮箱已被其他账号使用");
        }
    }

    private boolean canReadUser(User current, User target) {
        if (current.getId().equals(target.getId())) {
            return true;
        }
        if (RoleEnum.isSuperAdmin(current.getRole())) {
            return true;
        }
        return RoleEnum.ROLE_ADMIN.name().equals(current.getRole())
                && RoleEnum.ROLE_USER.name().equals(target.getRole());
    }

    private boolean canManageUser(User current, User target) {
        if (RoleEnum.isSuperAdmin(current.getRole())) {
            return current.getId().equals(target.getId()) || !RoleEnum.isSuperAdmin(target.getRole());
        }
        return RoleEnum.ROLE_USER.name().equals(target.getRole());
    }

    private record ProfileData(String nickname, String email, String phone, String address) {
    }
}
