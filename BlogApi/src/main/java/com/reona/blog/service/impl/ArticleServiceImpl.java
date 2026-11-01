package com.reona.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.reona.blog.commcn.Result;
import com.reona.blog.entity.Article;
import com.reona.blog.entity.ArticleLike;
import com.reona.blog.entity.Comment;
import com.reona.blog.entity.CommentLike;
import com.reona.blog.mapper.ArticleLikeMapper;
import com.reona.blog.mapper.ArticleMapper;
import com.reona.blog.mapper.CommentLikeMapper;
import com.reona.blog.mapper.CommentMapper;
import com.reona.blog.service.IArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;


@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements IArticleService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Override
    public Result findAll() {
        return Result.success(baseMapper.selectSummaries());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteWithRelated(List<Integer> ids) {
        if (ids == null) {
            return false;
        }
        List<Integer> articleIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (articleIds.isEmpty()) {
            return false;
        }
        List<Comment> comments = commentMapper.selectList(
                new QueryWrapper<Comment>().in("article_id", articleIds));
        List<Integer> commentIds = comments.stream().map(Comment::getId).toList();
        if (!commentIds.isEmpty()) {
            commentLikeMapper.delete(new QueryWrapper<CommentLike>().in("comment_id", commentIds));
            commentMapper.update(null, new UpdateWrapper<Comment>()
                    .in("id", commentIds)
                    .set("is_delete", true));
        }
        articleLikeMapper.delete(new QueryWrapper<ArticleLike>().in("article_id", articleIds));
        return removeByIds(articleIds);
    }
}
