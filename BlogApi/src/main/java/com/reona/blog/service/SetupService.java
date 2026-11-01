package com.reona.blog.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.reona.blog.commcn.Constants;
import com.reona.blog.commcn.RoleEnum;
import com.reona.blog.controller.dto.SetupRequestDTO;
import com.reona.blog.controller.dto.SetupStatusDTO;
import com.reona.blog.controller.dto.UserDTO;
import com.reona.blog.entity.Article;
import com.reona.blog.entity.ArticleLike;
import com.reona.blog.entity.AssistantSetting;
import com.reona.blog.entity.CommentLike;
import com.reona.blog.entity.SiteSetting;
import com.reona.blog.entity.User;
import com.reona.blog.exception.ServiceException;
import com.reona.blog.mapper.ArticleLikeMapper;
import com.reona.blog.mapper.ArticleMapper;
import com.reona.blog.mapper.CommentLikeMapper;
import com.reona.blog.mapper.SiteSettingMapper;
import com.reona.blog.utils.AssistantUrlValidator;
import com.reona.blog.utils.PasswordUtils;
import com.reona.blog.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class SetupService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final String SEED_ADMIN_USERNAME = "defaultadmin";
    private static final String SEED_ADMIN_EMAIL = "admin@example.com";

    @Resource
    private IUserService userService;

    @Resource
    private SiteSettingMapper siteSettingMapper;

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private ArticleLikeMapper articleLikeMapper;

    @Resource
    private CommentLikeMapper commentLikeMapper;

    @Resource
    private AssistantSettingService assistantSettingService;

    public SetupStatusDTO status() {
        SiteSetting site = siteSettingMapper.selectById(1);
        String siteName = site == null || StrUtil.isBlank(site.getSiteName())
                ? "Default Blog" : site.getSiteName().trim();
        String email = site == null || StrUtil.isBlank(site.getEmail())
                ? "default@example.com" : site.getEmail().trim();
        return new SetupStatusDTO(!hasSuperAdmin(), siteName, email);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserDTO initialize(SetupRequestDTO request) {
        if (request == null) {
            throw new ServiceException(Constants.CODE_400, "参数错误");
        }
        if (hasSuperAdmin()) {
            throw new ServiceException(Constants.CODE_600, "站点已经完成初始化");
        }

        String siteName = trim(request.getSiteName());
        if (siteName.isEmpty() || siteName.length() > 32) {
            throw new ServiceException(Constants.CODE_400, "站点名称长度需要在 1–32 个字符之间");
        }
        String siteEmail = trim(request.getSiteEmail());
        if (!isEmail(siteEmail)) {
            throw new ServiceException(Constants.CODE_400, "请填写有效的联系邮箱");
        }
        String username = trim(request.getOwnerUsername());
        if (username.isEmpty() || username.length() > 32) {
            throw new ServiceException(Constants.CODE_400, "用户名长度需要在 1–32 个字符之间");
        }
        if (username.contains("@")) {
            throw new ServiceException(Constants.CODE_400, "用户名不能包含 @");
        }
        String email = trim(request.getOwnerEmail());
        if (!isEmail(email)) {
            throw new ServiceException(Constants.CODE_400, "请填写有效的登录邮箱");
        }
        String nickname = trim(request.getOwnerNickname());
        if (nickname.isEmpty()) {
            nickname = username;
        }
        if (nickname.length() > 32) {
            throw new ServiceException(Constants.CODE_400, "昵称长度不能超过 32 个字符");
        }
        PasswordUtils.validate(request.getOwnerPassword());

        if (Boolean.TRUE.equals(request.getConfigureAssistant())) {
            validateAssistant(request);
        }

        User byUsername = userService.getOne(
                new QueryWrapper<User>().eq("username", username), false);
        User byEmail = userService.getOne(
                new QueryWrapper<User>().eq("email", email), false);
        if (byUsername != null && byEmail != null
                && !byUsername.getId().equals(byEmail.getId())) {
            throw new ServiceException(Constants.CODE_600, "用户名和邮箱已被不同账号使用");
        }
        User existing = byUsername != null ? byUsername : byEmail;
        if (existing != null && !isSeedAdmin(existing)) {
            throw new ServiceException(Constants.CODE_600, "用户名或邮箱已被使用");
        }
        User seedAdmin = userService.getOne(
                new QueryWrapper<User>().eq("username", SEED_ADMIN_USERNAME), false);
        if (seedAdmin == null) {
            seedAdmin = userService.getOne(
                    new QueryWrapper<User>().eq("email", SEED_ADMIN_EMAIL), false);
        }
        boolean ownerIsSeed = existing != null;

        String hashedPassword = PasswordUtils.hash(request.getOwnerPassword());
        User owner;
        boolean creating = existing == null;
        if (creating) {
            owner = new User();
            owner.setAvatarUrl("/file/头像.jpg");
        } else {
            owner = existing;
        }
        owner.setUsername(username);
        owner.setEmail(email);
        owner.setNickname(nickname);
        owner.setPassword(hashedPassword);
        owner.setRole(RoleEnum.ROLE_SUPER_ADMIN.name());
        try {
            if (creating) {
                if (!userService.save(owner)) {
                    throw new ServiceException(Constants.CODE_500, "初始化失败");
                }
            } else if (!userService.updateById(owner)) {
                throw new ServiceException(Constants.CODE_500, "初始化失败");
            }
        } catch (DuplicateKeyException e) {
            throw new ServiceException(Constants.CODE_600, "站点已经完成初始化或账号已被使用");
        }
        if (owner.getId() == null) {
            throw new ServiceException(Constants.CODE_500, "初始化失败");
        }

        SiteSetting site = siteSettingMapper.selectById(1);
        if (site == null) {
            site = new SiteSetting();
            site.setId(1);
            site.setSiteName(siteName);
            site.setEmail(siteEmail);
            siteSettingMapper.insert(site);
        } else {
            site.setSiteName(siteName);
            site.setEmail(siteEmail);
            siteSettingMapper.updateById(site);
        }

        if (Boolean.TRUE.equals(request.getConfigureAssistant())) {
            AssistantSetting assistant = assistantSettingService.get();
            assistant.setApiUrl(trim(request.getAssistantApiUrl()));
            assistant.setModel(trim(request.getAssistantModel()));
            assistant.setEnabled(request.getAssistantEnabled() == null || request.getAssistantEnabled());
            String apiKey = trim(request.getAssistantApiKey());
            if (!apiKey.isEmpty()) {
                assistant.setApiKey(apiKey);
            }
            assistantSettingService.update(assistant);
        }

        if (Boolean.TRUE.equals(request.getCreateSampleContent())
                && articleMapper.selectCount(null) == 0) {
            insertSample("Default Article 1");
            insertSample("Default Article 2");
            insertSample("Default Article 3");
        }

        if (!ownerIsSeed && seedAdmin != null && !seedAdmin.getId().equals(owner.getId())) {
            articleLikeMapper.delete(new QueryWrapper<ArticleLike>().eq("user_id", seedAdmin.getId()));
            commentLikeMapper.delete(new QueryWrapper<CommentLike>().eq("user_id", seedAdmin.getId()));
            userService.removeById(seedAdmin.getId());
        }

        UserDTO result = new UserDTO();
        BeanUtil.copyProperties(owner, result, true);
        result.setPassword(null);
        result.setToken(TokenUtils.genToken(owner.getId().toString(), hashedPassword));
        return result;
    }

    private void validateAssistant(SetupRequestDTO request) {
        String apiUrl = trim(request.getAssistantApiUrl());
        if (!AssistantUrlValidator.isValid(apiUrl)) {
            throw new ServiceException(Constants.CODE_400, "请填写有效的 HTTP 接口地址");
        }
        if (AssistantUrlValidator.isBlocked(apiUrl)) {
            throw new ServiceException(Constants.CODE_400, "接口地址不允许指向云元数据或链路本地地址");
        }
        String model = trim(request.getAssistantModel());
        if (model.isEmpty() || model.length() > 128) {
            throw new ServiceException(Constants.CODE_400, "请填写模型名称（不超过 128 个字符）");
        }
        String apiKey = trim(request.getAssistantApiKey());
        if (apiKey.length() > 255) {
            throw new ServiceException(Constants.CODE_400, "API Key 不能超过 255 个字符");
        }
    }

    private void insertSample(String title) {
        Article article = new Article();
        article.setTitle(title);
        article.setLabel("Default");
        article.setContent("Default article content for testing.");
        article.setEnable(true);
        articleMapper.insert(article);
    }

    private boolean hasSuperAdmin() {
        return userService.count(new QueryWrapper<User>()
                .eq("role", RoleEnum.ROLE_SUPER_ADMIN.name())) > 0;
    }

    private boolean isSeedAdmin(User user) {
        return SEED_ADMIN_USERNAME.equals(user.getUsername())
                || SEED_ADMIN_EMAIL.equalsIgnoreCase(user.getEmail());
    }

    private boolean isEmail(String value) {
        return !value.isEmpty() && value.length() <= 128 && EMAIL_PATTERN.matcher(value).matches();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
