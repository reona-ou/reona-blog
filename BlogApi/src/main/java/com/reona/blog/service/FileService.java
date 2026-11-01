package com.reona.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.reona.blog.commcn.Result;
import com.reona.blog.entity.Files;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface FileService extends IService<Files> {

    Result receptionAll();

    String upload(MultipartFile file) throws IOException;

    void download(String fileUUID, HttpServletResponse response) throws IOException;


    Page<Files> findPage(Integer pageNum, Integer pageSize, String name);

    Result delete(Integer id);

    Result deleteBatch(List<Integer> ids);

    Result updateByFile(Files file);
}
