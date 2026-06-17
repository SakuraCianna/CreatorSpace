package com.creatorspace.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 角色表实体，用于后台权限判断和用户身份标记。
 */
@TableName("roles")
public class RoleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    // 获取id。
    public Long getId() {
        return id;
    }

    // 设置id。
    public void setId(Long id) {
        this.id = id;
    }

    // 获取code。
    public String getCode() {
        return code;
    }

    // 设置code。
    public void setCode(String code) {
        this.code = code;
    }

    // 获取name。
    public String getName() {
        return name;
    }

    // 设置name。
    public void setName(String name) {
        this.name = name;
    }
}
