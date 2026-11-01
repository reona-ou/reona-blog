package com.reona.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reona.blog.commcn.Constants;
import com.reona.blog.commcn.Result;
import com.reona.blog.commcn.RoleEnum;
import com.reona.blog.config.AuthAccess;
import com.reona.blog.entity.Files;
import com.reona.blog.entity.User;
import com.reona.blog.service.FileService;
import com.reona.blog.utils.TokenUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 文件处理接口
 */
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private FileService fileService;

    @GetMapping("/reception/all")
    public Result receptionAll() {
        if (!isAdmin()) {
            return Result.error(Constants.CODE_403, "无权限查看文件");
        }
        return fileService.receptionAll();
    }


    /**
     * 文件上传接口
     *
     * @param file
     * @return Result
     * @throws
     */
    @PostMapping("/upload")
    public Result upload(@RequestParam MultipartFile file) throws IOException {
        if (!isAdmin()) {
            return Result.error(Constants.CODE_403, "无权限上传文件");
        }
        return Result.success(fileService.upload(file));
    }

    @AuthAccess
    @GetMapping("/{fileUUID}")
    public void download(@PathVariable String fileUUID, HttpServletResponse response) throws IOException {
        fileService.download(fileUUID, response);

    }


    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize,
                           @RequestParam(defaultValue = "") String name) {
        if (!isAdmin()) {
            return Result.error(Constants.CODE_403, "无权限查看文件");
        }
        Page<Files> filesPage = fileService.findPage(pageNum, pageSize, name);
        return Result.success(filesPage);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        if (!isAdmin()) {
            return Result.error(Constants.CODE_403, "无权限删除文件");
        }
        return fileService.delete(id);
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        if (!isAdmin()) {
            return Result.error(Constants.CODE_403, "无权限删除文件");
        }
        return fileService.deleteBatch(ids);

    }

    @PostMapping("/update")
    public Result update(@RequestBody Files file) {
        if (!isAdmin()) {
            return Result.error(Constants.CODE_403, "无权限修改文件");
        }
        return fileService.updateByFile(file);
    }

    private boolean isAdmin() {
        User user = TokenUtils.getVerifiedCurrentUser();
        return user != null && RoleEnum.isAdmin(user.getRole());
    }

}
