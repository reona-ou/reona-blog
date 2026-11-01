package com.reona.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.reona.blog.commcn.Constants;
import com.reona.blog.commcn.Result;
import com.reona.blog.commcn.RoleEnum;
import com.reona.blog.config.AuthAccess;
import com.reona.blog.entity.Article;
import com.reona.blog.entity.Comment;
import com.reona.blog.entity.CommentLike;
import com.reona.blog.entity.User;
import com.reona.blog.mapper.CommentLikeMapper;
import com.reona.blog.mapper.CommentMapper;
import com.reona.blog.service.IArticleService;
import com.reona.blog.service.RateLimiter;
import com.reona.blog.utils.TokenUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private CommentLikeMapper commentLikeMapper;

    @Resource
    private IArticleService articleService;

    @Resource
    private RateLimiter rateLimiter;

    private static final int COMMENT_MAX_PER_MINUTE = 10;

    @AuthAccess
    @GetMapping("/article/{articleId}")
    public Result list(@PathVariable Integer articleId) {
        Article article = articleService.getById(articleId);
        if (article == null || (!Boolean.TRUE.equals(article.getEnable()) && !isAdmin())) {
            return Result.success(Collections.emptyList());
        }
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("article_id", articleId);
        queryWrapper.eq("is_delete", false);
        queryWrapper.orderByDesc("id");
        List<Comment> comments = commentMapper.selectList(queryWrapper);
        if (comments.isEmpty()) {
            return Result.success(comments);
        }
        List<Integer> commentIds = comments.stream().map(Comment::getId).toList();

        QueryWrapper<CommentLike> countQuery = new QueryWrapper<>();
        countQuery.select("comment_id AS commentId", "COUNT(*) AS cnt")
                .in("comment_id", commentIds)
                .groupBy("comment_id");
        List<Map<String, Object>> countRows = commentLikeMapper.selectMaps(countQuery);
        Map<Integer, Long> likeCounts = new HashMap<>();
        for (Map<String, Object> row : countRows) {
            Object idValue = row.get("commentId");
            Object countValue = row.get("cnt");
            if (idValue instanceof Number id && countValue instanceof Number count) {
                likeCounts.put(id.intValue(), count.longValue());
            }
        }

        User currentUser = TokenUtils.getVerifiedCurrentUser();
        Set<Integer> likedIds = new HashSet<>();
        if (currentUser != null) {
            QueryWrapper<CommentLike> likedQuery = new QueryWrapper<>();
            likedQuery.select("comment_id")
                    .in("comment_id", commentIds)
                    .eq("user_id", currentUser.getId());
            for (Object value : commentLikeMapper.selectObjs(likedQuery)) {
                if (value instanceof Number id) {
                    likedIds.add(id.intValue());
                }
            }
        }

        for (Comment comment : comments) {
            comment.setLikeCount(likeCounts.getOrDefault(comment.getId(), 0L));
            comment.setLiked(likedIds.contains(comment.getId()));
        }
        return Result.success(comments);
    }

    @PostMapping
    public Result save(@RequestBody Comment comment) {
        User user = TokenUtils.getCurrentUser();
        if (user == null) {
            return Result.error(Constants.CODE_401, "请先登录后再评论");
        }
        if (comment.getArticleId() == null) {
            return Result.error("文章不存在");
        }
        Article article = articleService.getById(comment.getArticleId());
        if (article == null || (!Boolean.TRUE.equals(article.getEnable()) && !isAdmin())) {
            return Result.error(Constants.CODE_404, "文章不存在");
        }
        String content = comment.getContent() == null ? "" : comment.getContent().trim();
        if (content.isEmpty()) {
            return Result.error("评论不能为空");
        }
        if (content.length() > 500) {
            return Result.error("评论不能超过 500 字");
        }
        if (!rateLimiter.tryAcquire("comment|" + user.getId(), COMMENT_MAX_PER_MINUTE, 60_000L)) {
            return Result.error(Constants.CODE_429, "评论过于频繁，请稍后再试");
        }

        Integer parentId = comment.getParentId();
        if (parentId != null && parentId > 0) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null || Boolean.TRUE.equals(parent.getIsDelete())
                    || !comment.getArticleId().equals(parent.getArticleId())) {
                return Result.error("回复的评论不存在");
            }
            if (parent.getParentId() != null && parent.getParentId() > 0) {
                parentId = parent.getParentId();
            }
            comment.setParentId(parentId);
        } else {
            comment.setParentId(0);
        }

        comment.setId(null);
        comment.setCreateTime(null);
        comment.setUserId(user.getId());
        comment.setUsername(user.getUsername());
        comment.setNickname(user.getNickname() == null || user.getNickname().trim().isEmpty()
                ? user.getUsername() : user.getNickname());
        comment.setContent(content);
        comment.setIsDelete(false);
        commentMapper.insert(comment);
        Comment saved = commentMapper.selectById(comment.getId());
        if (saved == null) {
            return Result.error(Constants.CODE_500, "评论保存失败");
        }
        saved.setLikeCount(0L);
        saved.setLiked(false);
        return Result.success(saved);
    }

    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result delete(@PathVariable Integer id) {
        User user = TokenUtils.getCurrentUser();
        if (user == null) {
            return Result.error(Constants.CODE_401, "请先登录");
        }
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            return Result.error("评论不存在");
        }
        boolean admin = RoleEnum.isAdmin(user.getRole());
        if (!admin && !user.getId().equals(comment.getUserId())) {
            return Result.error(Constants.CODE_403, "只能删除自己的评论");
        }
        commentMapper.update(null, new UpdateWrapper<Comment>()
                .eq("id", id)
                .set("is_delete", true));
        commentMapper.update(null, new UpdateWrapper<Comment>()
                .eq("parent_id", id)
                .eq("is_delete", false)
                .set("is_delete", true));
        return Result.success();
    }

    @PostMapping("/{id}/like")
    public Result like(@PathVariable Integer id) {
        User user = TokenUtils.getCurrentUser();
        if (user == null) {
            return Result.error(Constants.CODE_401, "请先登录");
        }
        Comment comment = commentMapper.selectById(id);
        if (comment == null || Boolean.TRUE.equals(comment.getIsDelete())) {
            return Result.error("评论不存在");
        }
        QueryWrapper<CommentLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("comment_id", id);
        queryWrapper.eq("user_id", user.getId());
        CommentLike existing = commentLikeMapper.selectOne(queryWrapper);
        if (existing != null) {
            commentLikeMapper.deleteById(existing.getId());
        } else {
            CommentLike like = new CommentLike();
            like.setCommentId(id);
            like.setUserId(user.getId());
            try {
                commentLikeMapper.insert(like);
            } catch (DuplicateKeyException ignored) {
                // 并发重复点赞时保持已点赞状态
            }
        }
        return Result.success(likeStatus(id, user));
    }

    private Map<String, Object> likeStatus(Integer commentId, User user) {
        QueryWrapper<CommentLike> countQuery = new QueryWrapper<>();
        countQuery.eq("comment_id", commentId);
        Long count = commentLikeMapper.selectCount(countQuery);
        countQuery.eq("user_id", user.getId());
        boolean liked = commentLikeMapper.selectCount(countQuery) > 0;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("liked", liked);
        data.put("count", count);
        return data;
    }

    @GetMapping("/count")
    public Result count() {
        User user = TokenUtils.getCurrentUser();
        if (user == null || !RoleEnum.isAdmin(user.getRole())) {
            return Result.error(Constants.CODE_403, "无权限");
        }
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", false);
        return Result.success(commentMapper.selectCount(queryWrapper));
    }

    private boolean isAdmin() {
        User user = TokenUtils.getVerifiedCurrentUser();
        return user != null && RoleEnum.isAdmin(user.getRole());
    }
}
