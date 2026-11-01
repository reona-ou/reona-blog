package com.reona.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.reona.blog.commcn.Constants;
import com.reona.blog.commcn.Result;
import com.reona.blog.commcn.RoleEnum;
import com.reona.blog.config.AuthAccess;
import com.reona.blog.entity.Article;
import com.reona.blog.entity.ArticleLike;
import com.reona.blog.entity.User;
import com.reona.blog.mapper.ArticleLikeMapper;
import com.reona.blog.mapper.ArticleMapper;
import com.reona.blog.utils.TokenUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/article-like")
public class ArticleLikeController {

    @Resource
    private ArticleLikeMapper articleLikeMapper;

    @Resource
    private ArticleMapper articleMapper;

    @AuthAccess
    @GetMapping("/{articleId}")
    public Result get(@PathVariable Integer articleId) {
        return Result.success(status(articleId, TokenUtils.getVerifiedCurrentUser()));
    }

    @PostMapping("/{articleId}")
    public Result toggle(@PathVariable Integer articleId) {
        User user = TokenUtils.getCurrentUser();
        if (user == null) {
            return Result.error(Constants.CODE_401, "请先登录");
        }
        Article article = articleMapper.selectById(articleId);
        if (article == null
                || (!Boolean.TRUE.equals(article.getEnable()) && !RoleEnum.isAdmin(user.getRole()))) {
            return Result.error("文章不存在");
        }
        QueryWrapper<ArticleLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("article_id", articleId);
        queryWrapper.eq("user_id", user.getId());
        ArticleLike existing = articleLikeMapper.selectOne(queryWrapper);
        if (existing != null) {
            articleLikeMapper.deleteById(existing.getId());
        } else {
            ArticleLike like = new ArticleLike();
            like.setArticleId(articleId);
            like.setUserId(user.getId());
            try {
                articleLikeMapper.insert(like);
            } catch (DuplicateKeyException ignored) {
                // 并发重复点赞时保持已点赞状态
            }
        }
        return Result.success(status(articleId, user));
    }

    private Map<String, Object> status(Integer articleId, User user) {
        QueryWrapper<ArticleLike> countQuery = new QueryWrapper<>();
        countQuery.eq("article_id", articleId);
        Long count = articleLikeMapper.selectCount(countQuery);
        boolean liked = false;
        if (user != null) {
            countQuery.eq("user_id", user.getId());
            liked = articleLikeMapper.selectCount(countQuery) > 0;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("liked", liked);
        data.put("count", count);
        return data;
    }
}
