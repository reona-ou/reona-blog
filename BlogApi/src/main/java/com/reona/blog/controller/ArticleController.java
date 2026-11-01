package com.reona.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reona.blog.commcn.Constants;
import com.reona.blog.commcn.Result;
import com.reona.blog.commcn.RoleEnum;
import com.reona.blog.config.AuthAccess;
import com.reona.blog.entity.Article;
import com.reona.blog.entity.User;
import com.reona.blog.exception.ServiceException;
import com.reona.blog.utils.TokenUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.reona.blog.service.IArticleService;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/article")
public class ArticleController {

    @Resource
    private IArticleService articleService;

    // 新增或者更新
    @PostMapping
    public Result save(@RequestBody Article article) {
        if (!isAdmin()) {
            return Result.error(Constants.CODE_403, "无权限操作文章");
        }
        if (article == null) {
            throw new ServiceException(Constants.CODE_400, "参数错误");
        }
        String title = article.getTitle() == null ? "" : article.getTitle().trim();
        if (title.isEmpty() || title.length() > 32) {
            throw new ServiceException(Constants.CODE_400, "标题长度需要在 1–32 个字符之间");
        }
        String label = article.getLabel() == null ? "" : article.getLabel().trim();
        if (label.isEmpty()) {
            label = "随笔";
        }
        if (label.length() > 32) {
            throw new ServiceException(Constants.CODE_400, "标签长度不能超过 32 个字符");
        }
        String content = article.getContent() == null ? "" : article.getContent();
        if (content.trim().isEmpty()) {
            throw new ServiceException(Constants.CODE_400, "请填写文章内容");
        }
        if (content.getBytes(StandardCharsets.UTF_8).length > 60000) {
            throw new ServiceException(Constants.CODE_400, "文章内容过长，请控制在 60000 字节以内");
        }
        article.setTitle(title);
        article.setLabel(label);
        article.setContent(content);
        if (article.getId() == null) {
            article.setEnable(article.getEnable() == null || article.getEnable());
        } else if (article.getEnable() == null) {
            Article existing = articleService.getById(article.getId());
            article.setEnable(existing == null || !Boolean.FALSE.equals(existing.getEnable()));
        }
        article.setCreateTime(null);
        article.setUpdateTime(null);
        return Result.success(articleService.saveOrUpdate(article));
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        if (!isAdmin()) {
            return Result.error(Constants.CODE_403, "无权限操作文章");
        }
        if (articleService.getById(id) == null) {
            return Result.error(Constants.CODE_404, "文章不存在");
        }
        return Result.success(articleService.deleteWithRelated(List.of(id)));
    }


    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        if (!isAdmin()) {
            return Result.error(Constants.CODE_403, "无权限操作文章");
        }
        return Result.success(articleService.deleteWithRelated(ids));
    }

    @AuthAccess
    @GetMapping
    public Result findAll() {
        return articleService.findAll();
    }

    @AuthAccess
    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        Article article = articleService.getById(id);
        if (article == null || (!Boolean.TRUE.equals(article.getEnable()) && !isAdmin())) {
            return Result.success(null);
        }
        return Result.success(article);
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!isAdmin()) {
            return Result.error(Constants.CODE_403, "无权限查看文章列表");
        }
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 100);
        QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        return Result.success(articleService.page(new Page<>(safePageNum, safePageSize), queryWrapper));
    }

    @GetMapping("/stats")
    public Result stats() {
        if (!isAdmin()) {
            return Result.error(Constants.CODE_403, "无权限查看文章统计");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", articleService.count());
        data.put("published", articleService.count(new QueryWrapper<Article>().eq("enable", true)));
        return Result.success(data);
    }

    private boolean isAdmin() {
        User user = TokenUtils.getVerifiedCurrentUser();
        return user != null && RoleEnum.isAdmin(user.getRole());
    }
}

