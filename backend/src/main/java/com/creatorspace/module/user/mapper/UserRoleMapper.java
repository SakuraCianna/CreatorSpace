package com.creatorspace.module.user.mapper;

import org.apache.ibatis.annotations.Insert;

/**
 * 用户与角色关联表数据访问接口。
 */
public interface UserRoleMapper {

    @Insert("""
            insert into user_roles (user_id, role_id)
            values (#{userId}, #{roleId})
            on conflict (user_id, role_id) do nothing
            """)
    // 插入关联记录并忽略重复数据。
    int insertIgnore(Long userId, Long roleId);
}
