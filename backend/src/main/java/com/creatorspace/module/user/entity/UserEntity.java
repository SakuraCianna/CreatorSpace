package com.creatorspace.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

/**
 * 用户表实体，承载账号状态和密码哈希等认证数据。
 */
@TableName("users")
public class UserEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String nickname;

    @TableField("password_hash")
    private String passwordHash;

    private String status;

    @TableField("avatar_url")
    private String avatarUrl;

    private String bio;

    private String email;

    @TableField("last_login_at")
    private OffsetDateTime lastLoginAt;

    @TableField("failed_login_attempts")
    private Integer failedLoginAttempts;

    @TableField("account_locked_until")
    private OffsetDateTime accountLockedUntil;

    // 获取id。
    public Long getId() {
        return id;
    }

    // 设置id。
    public void setId(Long id) {
        this.id = id;
    }

    // 获取username。
    public String getUsername() {
        return username;
    }

    // 设置username。
    public void setUsername(String username) {
        this.username = username;
    }

    // 获取nickname。
    public String getNickname() {
        return nickname;
    }

    // 设置nickname。
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    // 获取password hash。
    public String getPasswordHash() {
        return passwordHash;
    }

    // 设置password hash。
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    // 获取账号状态。
    public String getStatus() {
        return status;
    }

    // 设置账号状态。
    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // 获取last login at。
    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    // 设置last login at。
    public void setLastLoginAt(OffsetDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    // 获取失败登录次数。
    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    // 设置失败登录次数。
    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    // 获取账号锁定截止时间。
    public OffsetDateTime getAccountLockedUntil() {
        return accountLockedUntil;
    }

    // 设置账号锁定截止时间。
    public void setAccountLockedUntil(OffsetDateTime accountLockedUntil) {
        this.accountLockedUntil = accountLockedUntil;
    }
}
