package com.reona.blog.commcn;

//枚举类型
public enum RoleEnum {

    ROLE_SUPER_ADMIN, ROLE_ADMIN, ROLE_USER;

    public static boolean isAdmin(String role) {
        return ROLE_SUPER_ADMIN.name().equals(role) || ROLE_ADMIN.name().equals(role);
    }

    public static boolean isSuperAdmin(String role) {
        return ROLE_SUPER_ADMIN.name().equals(role);
    }
}
