package com.creatorspace.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.creatorspace.module.user.entity.UserEntity;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户表数据访问接口。
 */
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("""
            select r.code
            from roles r
            join user_roles ur on ur.role_id = r.id
            where ur.user_id = #{userId}
            order by r.code
            """)
    // 查询用户拥有的角色编码。
    List<String> selectRoleCodesByUserId(Long userId);
}
