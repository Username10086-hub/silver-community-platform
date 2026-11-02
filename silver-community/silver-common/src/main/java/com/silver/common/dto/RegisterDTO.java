package com.silver.common.dto;

import lombok.Data;
import java.util.List;

@Data
public class RegisterDTO {
    // 通用字段
    private String username;
    private String password;
    private String realName;
    private String phone;
    private Integer role;
    private Integer age;
    private Integer gender;

    // 老人专属字段
    private String address;
    private Integer livingStatus;
    private Integer mobility;
    private String chronicDisease;
    private String dietaryRestriction;
    private String emergencyPhone;

    // 由子女代注册时，传当前登录的子女ID
    private Long currentGuardianId;

    // 老人自注册时手动填写的子女绑定列表
    private List<GuardianBinding> guardianBindings;

    @Data
    public static class GuardianBinding {
        private Long guardianId;
        private String guardianName;
    }
}