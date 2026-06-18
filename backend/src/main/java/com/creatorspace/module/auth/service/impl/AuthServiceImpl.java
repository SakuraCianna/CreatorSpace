package com.creatorspace.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.creatorspace.common.constant.ContentConstants;
import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.module.auth.dto.LoginRequest;
import com.creatorspace.module.auth.dto.RegisterRequest;
import com.creatorspace.module.auth.service.AuthService;
import com.creatorspace.module.auth.vo.AuthTokenVO;
import com.creatorspace.module.auth.vo.UserSummaryVO;
import com.creatorspace.module.user.entity.RoleEntity;
import com.creatorspace.module.user.entity.UserEntity;
import com.creatorspace.module.user.mapper.RoleMapper;
import com.creatorspace.module.user.mapper.UserMapper;
import com.creatorspace.module.user.mapper.UserRoleMapper;
import com.creatorspace.security.JwtService;
import com.creatorspace.security.LoginUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 认证业务实现，负责用户注册、用户登录和管理员登录令牌签发。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // 通过构造器注入认证所需的 Mapper、密码编码器和令牌服务。
    public AuthServiceImpl(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // 注册普通用户并返回安全用户信息。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserSummaryVO register(RegisterRequest request) {
        String username = request.username().trim();
        ensureUsernameAvailable(username);

        RoleEntity role = requiredRole(ContentConstants.ROLE_USER);
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setNickname(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(ContentConstants.USER_ACTIVE);
        userMapper.insert(user);
        userRoleMapper.insertIgnore(user.getId(), role.getId());

        return new UserSummaryVO(user.getId(), user.getUsername(), List.of(ContentConstants.ROLE_USER));
    }

    // 校验普通用户身份并签发访问令牌。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenVO login(LoginRequest request) {
        return loginAndIssueToken(request, false);
    }

    // 校验管理员身份并签发访问令牌。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenVO loginAdmin(LoginRequest request) {
        return loginAndIssueToken(request, true);
    }

    // 校验账号密码、状态和角色要求，然后签发访问令牌。
    private AuthTokenVO loginAndIssueToken(LoginRequest request, boolean adminRequired) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, request.username().trim()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!ContentConstants.USER_ACTIVE.equals(user.getStatus())) {
            throw BusinessException.forbidden("当前用户已被禁用");
        }
        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        if (adminRequired && !roles.contains(ContentConstants.ROLE_ADMIN)) {
            throw BusinessException.forbidden("当前用户没有后台权限");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userMapper.updateById(user);
        UserSummaryVO summary = new UserSummaryVO(user.getId(), user.getUsername(), roles);
        String accessToken = jwtService.createAccessToken(new LoginUser(user.getId(), user.getUsername(), roles));
        return new AuthTokenVO(accessToken, summary);
    }

    // 确认用户名未被占用。
    private void ensureUsernameAvailable(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username));
        if (count > 0) {
            throw BusinessException.conflict("用户名已存在");
        }
    }

    // 查询必须存在的系统角色。
    private RoleEntity requiredRole(String code) {
        RoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, code));
        if (role == null) {
            throw BusinessException.badRequest("系统角色未初始化");
        }
        return role;
    }
}
