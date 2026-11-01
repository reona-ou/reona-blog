package com.reona.blog.mapper;

import com.reona.blog.controller.dto.ArticleSummaryDTO;
import com.reona.blog.entity.Article;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    @Select("""
            SELECT id, label, title, enable,
                   create_time AS createTime, update_time AS updateTime,
                   LEFT(content, 3000) AS summary
            FROM sys_article
            WHERE enable = 1
            ORDER BY create_time DESC
            """)
    List<ArticleSummaryDTO> selectSummaries();
}
