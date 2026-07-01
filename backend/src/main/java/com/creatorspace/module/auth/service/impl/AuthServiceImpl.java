package com.creatorspace.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.creatorspace.common.constant.ContentConstants;
import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.module.auth.dto.LoginRequest;
import com.creatorspace.module.auth.dto.RegisterRequest;
import com.creatorspace.module.auth.dto.ResetPasswordRequest;
import com.creatorspace.module.auth.service.AuthService;
import com.creatorspace.module.auth.vo.AuthTokenVO;
import com.creatorspace.module.auth.vo.UserSummaryVO;
import com.creatorspace.module.email.service.EmailVerificationService;
import com.creatorspace.module.user.entity.RoleEntity;
import com.creatorspace.module.user.entity.UserEntity;
import com.creatorspace.module.user.mapper.RoleMapper;
import com.creatorspace.module.user.mapper.UserMapper;
import com.creatorspace.module.user.mapper.UserRoleMapper;
import com.creatorspace.module.auth.service.HCaptchaService;
import com.creatorspace.security.JwtService;
import com.creatorspace.security.LoginUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * 认证业务实现，负责用户注册、用户登录和管理员登录令牌签发。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JdbcTemplate jdbcTemplate;
    private final EmailVerificationService emailVerificationService;
    private final SecureRandom secureRandom;
    private final long accessTokenExpireMinutes;
    private final long refreshTokenExpireDays;
    private final HCaptchaService hCaptchaService;

    public AuthServiceImpl(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JdbcTemplate jdbcTemplate,
            EmailVerificationService emailVerificationService,
            HCaptchaService hCaptchaService,
            @Value("${app.security.jwt-access-token-expire-minutes}") long accessTokenExpireMinutes,
            @Value("${app.security.jwt-refresh-token-expire-days}") long refreshTokenExpireDays
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jdbcTemplate = jdbcTemplate;
        this.emailVerificationService = emailVerificationService;
        this.hCaptchaService = hCaptchaService;
        this.secureRandom = new SecureRandom();
        this.accessTokenExpireMinutes = accessTokenExpireMinutes;
        this.refreshTokenExpireDays = refreshTokenExpireDays;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserSummaryVO register(RegisterRequest request) {
        String email = request.email().trim();
        if (!email.endsWith("@qq.com")) {
            throw BusinessException.badRequest("仅支持 QQ 邮箱注册");
        }
        emailVerificationService.verifyCode(email, request.verificationCode(), "REGISTER");

        String username = request.username().trim();
        ensureUsernameAvailable(username);
        ensureEmailAvailable(email);

        RoleEntity role = requiredRole(ContentConstants.ROLE_USER);
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setNickname(username);
        user.setEmail(email);
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

    // 使用刷新令牌获取新的访问令牌和刷新令牌。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenVO refresh(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        List<RefreshTokenRow> rows = jdbcTemplate.query("""
                        select user_id, expires_at, revoked_at
                        from refresh_tokens
                        where token_hash = ?
                        """,
                (rs, rowNum) -> new RefreshTokenRow(
                        rs.getLong("user_id"),
                        rs.getObject("expires_at", OffsetDateTime.class),
                        rs.getObject("revoked_at", OffsetDateTime.class)
                ),
                tokenHash);
        if (rows.isEmpty()) {
            throw BusinessException.unauthorized("刷新令牌不存在");
        }
        RefreshTokenRow row = rows.getFirst();
        if (row.revokedAt != null || row.expiresAt.isBefore(OffsetDateTime.now())) {
            throw BusinessException.unauthorized("刷新令牌已失效");
        }
        // 吊销旧刷新令牌
        jdbcTemplate.update("update refresh_tokens set revoked_at = now() where token_hash = ?", tokenHash);
        // 签发新令牌
        return issueTokens(row.userId);
    }

    // 登出，吊销指定的刷新令牌。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        int updated = jdbcTemplate.update(
                "update refresh_tokens set revoked_at = now() where token_hash = ? and revoked_at is null",
                tokenHash);
        if (updated == 0) {
            throw BusinessException.unauthorized("刷新令牌不存在或已失效");
        }
    }

    // 校验账号密码、状态和角色要求，然后签发令牌对。
    private AuthTokenVO loginAndIssueToken(LoginRequest request, boolean adminRequired) {
        if (!hCaptchaService.verify(request.hcaptchaToken())) {
            throw BusinessException.badRequest("人机验证失败，请重试");
        }

        String account = request.username().trim();
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, account)
                .or()
                .eq(UserEntity::getEmail, account));
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号不存在");
        }
        checkAccountLocked(user);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordFailedAttempt(user.getId());
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "密码错误");
        }
        if (!ContentConstants.USER_ACTIVE.equals(user.getStatus())) {
            throw BusinessException.forbidden("当前用户已被禁用");
        }
        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        if (adminRequired && !roles.contains(ContentConstants.ROLE_ADMIN)) {
            throw BusinessException.forbidden("当前用户没有后台权限");
        }
        resetFailedAttempts(user.getId());
        user.setLastLoginAt(OffsetDateTime.now());
        userMapper.updateById(user);
        return issueTokens(user.getId());
    }

    private void checkAccountLocked(UserEntity user) {
        if (user.getFailedLoginAttempts() == null || user.getFailedLoginAttempts() < MAX_FAILED_ATTEMPTS) {
            return;
        }
        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(OffsetDateTime.now())) {
            throw BusinessException.forbidden("账号已被锁定，请稍后再试");
        }
        // 锁定时间已过，重置计数
        resetFailedAttempts(user.getId());
    }

    private void recordFailedAttempt(Long userId) {
        jdbcTemplate.update("""
                        update users
                        set failed_login_attempts = failed_login_attempts + 1,
                            account_locked_until = case
                                when failed_login_attempts + 1 >= ? then now() + ? * interval '1 minute'
                                else account_locked_until
                            end
                        where id = ?
                        """,
                MAX_FAILED_ATTEMPTS, LOCK_DURATION_MINUTES, userId);
    }

    private void resetFailedAttempts(Long userId) {
        jdbcTemplate.update("""
                        update users
                        set failed_login_attempts = 0,
                            account_locked_until = null
                        where id = ?
                        """,
                userId);
    }

    private AuthTokenVO issueTokens(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        UserSummaryVO summary = new UserSummaryVO(user.getId(), user.getUsername(), roles);
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), roles);

        String accessToken = jwtService.createAccessToken(loginUser);
        String rawRefreshToken = generateRefreshToken();
        String tokenHash = hashToken(rawRefreshToken);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(refreshTokenExpireDays);
        jdbcTemplate.update("""
                        insert into refresh_tokens (user_id, token_hash, expires_at)
                        values (?, ?, ?)
                        """,
                userId, tokenHash, expiresAt);
        return new AuthTokenVO(accessToken, rawRefreshToken, accessTokenExpireMinutes * 60, summary);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashToken(String token) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Token hash failed", e);
        }
    }

    @Override
    public void sendVerificationCode(String email, String hcaptchaToken, String purpose) {
        if (!hCaptchaService.verify(hcaptchaToken)) {
            throw BusinessException.badRequest("人机验证失败，请重试");
        }
        emailVerificationService.sendCode(email, purpose);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.email().trim();
        emailVerificationService.verifyCode(email, request.verificationCode(), "RESET_PASSWORD");
        int updated = jdbcTemplate.update(
                "update users set password_hash = ? where email = ?",
                passwordEncoder.encode(request.newPassword()),
                email);
        if (updated == 0) {
            throw BusinessException.badRequest("该邮箱未注册");
        }
    }

    // 确认用户名未被占用。
    private void ensureUsernameAvailable(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username));
        if (count > 0) {
            throw BusinessException.conflict("用户名已存在");
        }
    }

    // 确认邮箱未被占用。
    private void ensureEmailAvailable(String email) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getEmail, email));
        if (count > 0) {
            throw BusinessException.conflict("该邮箱已被注册");
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

    private record RefreshTokenRow(Long userId, OffsetDateTime expiresAt, OffsetDateTime revokedAt) {
    }
}
