package com.reona.blog.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.reona.blog.commcn.Constants;
import com.reona.blog.commcn.Result;
import com.reona.blog.entity.Files;
import com.reona.blog.exception.ServiceException;
import com.reona.blog.mapper.FileMapper;
import com.reona.blog.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, Files> implements FileService {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "gif", "webp"));

    @Value("${files.upload.path}")
    String fileUploadPath;

    @Autowired
    private FileMapper fileMapper;

    @Override
    public Result receptionAll() {
        return Result.success(fileMapper.selectList(
                new QueryWrapper<Files>().eq("is_delete", false)));
    }

    @Override
    public String upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(Constants.CODE_400, "请选择要上传的图片");
        }
        String originalFilename = file.getOriginalFilename();
        String type = FileUtil.extName(originalFilename);
        String normalizedType = type == null ? "" : type.toLowerCase();
        if (!ALLOWED_IMAGE_TYPES.contains(normalizedType)) {
            throw new ServiceException(Constants.CODE_400, "只允许上传 jpg/jpeg/png/gif/webp 图片");
        }
        long size = file.getSize();
        if (size > MAX_IMAGE_SIZE) {
            throw new ServiceException(Constants.CODE_400, "图片大小不能超过 10MB");
        }

        String md5;
        try (InputStream input = file.getInputStream()) {
            md5 = SecureUtil.md5(input);
        }
        Files existing = getFileByMd5(md5);
        if (existing != null) {
            File existingFile = resolveUploadFile(existing.getUrl());
            if (existingFile != null && existingFile.isFile()) {
                return existing.getUrl();
            }
        }

        File uploadParentFile = uploadDir();
        if (!uploadParentFile.exists() && !uploadParentFile.mkdirs()) {
            throw new ServiceException(Constants.CODE_500, "上传目录创建失败");
        }
        String fileUUID = IdUtil.fastSimpleUUID() + StrUtil.DOT + normalizedType;
        File uploadFile = new File(uploadParentFile, fileUUID);
        String url = "/file/" + fileUUID;
        String name = safeFileName(originalFilename, fileUUID);

        try {
            file.transferTo(uploadFile);
            if (existing != null) {
                existing.setName(name);
                existing.setType(normalizedType);
                existing.setSize(size / 1024);
                existing.setUrl(url);
                existing.setMd5(md5);
                existing.setIsDelete(false);
                existing.setEnable(true);
                if (fileMapper.updateById(existing) == 0) {
                    throw new ServiceException(Constants.CODE_500, "文件保存失败");
                }
                return url;
            }

            Files saveFile = new Files();
            saveFile.setName(name);
            saveFile.setType(normalizedType);
            saveFile.setSize(size / 1024);
            saveFile.setUrl(url);
            saveFile.setMd5(md5);
            saveFile.setIsDelete(false);
            saveFile.setEnable(true);
            if (fileMapper.insert(saveFile) == 0) {
                throw new ServiceException(Constants.CODE_500, "文件保存失败");
            }
            return url;
        } catch (IOException | RuntimeException e) {
            FileUtil.del(uploadFile);
            throw e;
        }
    }

    private String safeFileName(String originalFilename, String fallback) {
        String name = originalFilename == null ? "" : originalFilename.trim();
        name = name.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        if (name.isEmpty()) {
            return fallback;
        }
        if (name.length() > 255) {
            int dot = name.lastIndexOf('.');
            String ext = dot > 0 && name.length() - dot <= 16 ? name.substring(dot) : "";
            name = name.substring(0, Math.max(1, 255 - ext.length())) + ext;
            if (name.length() > 255) {
                name = name.substring(0, 255);
            }
        }
        return name;
    }

    @Override
    public void download(String fileUUID, HttpServletResponse response) throws IOException {
        String ext = FileUtil.extName(fileUUID);
        if (StrUtil.isBlank(ext)
                || !ALLOWED_IMAGE_TYPES.contains(ext.toLowerCase())
                || fileUUID.contains("/")
                || fileUUID.contains("\\")
                || fileUUID.contains("..")) {
            throw new ServiceException(Constants.CODE_404, "文件不存在");
        }
        File uploadFile = new File(uploadDir(), fileUUID);
        if (!uploadFile.isFile()) {
            throw new ServiceException(Constants.CODE_404, "文件不存在");
        }

        String contentType = URLConnection.guessContentTypeFromName(fileUUID);
        response.setContentType(contentType == null ? "application/octet-stream" : contentType);
        response.addHeader("X-Content-Type-Options", "nosniff");
        response.addHeader("Content-Disposition", "inline;filename="
                + URLEncoder.encode(fileUUID, StandardCharsets.UTF_8));

        try (InputStream input = new FileInputStream(uploadFile)) {
            ServletOutputStream output = response.getOutputStream();
            StreamUtils.copy(input, output);
            output.flush();
        }
    }

    @Override
    public Page<Files> findPage(Integer pageNum, Integer pageSize, String name) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 100);
        Page<Files> page = new Page<>(safePageNum, safePageSize);
        QueryWrapper<Files> queryWrapper = new QueryWrapper<>();
        if (!"".equals(name)) {
            queryWrapper.like("name", name);
        }
        queryWrapper.eq("is_delete", false);
        queryWrapper.orderByDesc("id");
        return fileMapper.selectPage(page, queryWrapper);
    }

    @Override
    public Result delete(Integer id) {
        Files files = id == null ? null : fileMapper.selectById(id);
        if (files == null) {
            throw new ServiceException(Constants.CODE_404, "文件不存在");
        }
        files.setIsDelete(true);
        fileMapper.updateById(files);
        deletePhysicalIfUnused(files.getUrl());
        return Result.success(true);
    }

    @Override
    public Result deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success(true);
        }
        QueryWrapper<Files> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", ids);
        List<Files> filesList = fileMapper.selectList(queryWrapper);
        Set<String> urls = new HashSet<>();
        for (Files file : filesList) {
            file.setIsDelete(true);
            fileMapper.updateById(file);
            urls.add(file.getUrl());
        }
        urls.forEach(this::deletePhysicalIfUnused);
        return Result.success(true);
    }

    @Override
    public Result updateByFile(Files file) {
        return Result.success(fileMapper.updateById(file));
    }

    private File uploadDir() {
        return new File(fileUploadPath).getAbsoluteFile();
    }

    private Files getFileByMd5(String md5) {
        QueryWrapper<Files> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("md5", md5).eq("is_delete", false);
        List<Files> filesList = fileMapper.selectList(queryWrapper);
        return filesList.isEmpty() ? null : filesList.get(0);
    }

    private File resolveUploadFile(String url) {
        if (url == null || !url.startsWith("/file/")) {
            return null;
        }
        String filename = url.substring("/file/".length());
        if (filename.isBlank()
                || filename.contains("/")
                || filename.contains("\\")
                || filename.contains("..")) {
            return null;
        }
        return new File(uploadDir(), filename);
    }

    private void deletePhysicalIfUnused(String url) {
        File file = resolveUploadFile(url);
        if (file == null) {
            return;
        }
        long active = fileMapper.selectCount(new QueryWrapper<Files>()
                .eq("url", url)
                .eq("is_delete", false));
        if (active > 0) {
            return;
        }
        try {
            java.nio.file.Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            log.warn("删除上传文件失败: {}", file.getAbsolutePath(), e);
        }
    }
}
