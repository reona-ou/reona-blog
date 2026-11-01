package com.reona.blog.service;

import com.reona.blog.commcn.Result;
import com.reona.blog.entity.Article;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;


public interface IArticleService extends IService<Article> {

    Result findAll();

    boolean deleteWithRelated(List<Integer> ids);
}
