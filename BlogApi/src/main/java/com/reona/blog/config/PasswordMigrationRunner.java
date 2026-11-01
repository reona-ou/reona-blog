package com.reona.blog.config;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.reona.blog.entity.User;
import com.reona.blog.service.IUserService;
import com.reona.blog.utils.PasswordUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private static final Log LOG = Log.get();

    @Resource
    private IUserService userService;

    @Override
    public void run(String... args) {
        List<User> users = userService.list();
        int migrated = 0;
        for (User user : users) {
            String password = user.getPassword();
            if (password == null || password.isEmpty() || PasswordUtils.isHashed(password)) {
                continue;
            }
            String hashed = PasswordUtils.hash(password);
            if (userService.update(new UpdateWrapper<User>()
                    .eq("id", user.getId())
                    .set("password", hashed))) {
                migrated++;
            } else {
                LOG.error("密码迁移失败，用户ID: {}", user.getId());
            }
        }
        if (migrated > 0) {
            LOG.info("已将 {} 个历史明文密码迁移为 BCrypt", migrated);
        }
    }
}
