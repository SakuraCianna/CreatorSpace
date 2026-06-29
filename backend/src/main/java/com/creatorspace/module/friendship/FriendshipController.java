package com.creatorspace.module.friendship;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.security.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@Validated
@RestController
public class FriendshipController {

    private final JdbcTemplate jdbcTemplate;

    public FriendshipController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/me/friends")
    public ApiResponse<PageResponse<FriendVO>> listFriends(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        long offset = (page - 1) * pageSize;
        Long total = jdbcTemplate.queryForObject("""
                        select count(*) from user_friendships
                        where status = 'ACCEPTED'
                          and (requester_id = ? or addressee_id = ?)
                        """,
                Long.class, loginUser.userId(), loginUser.userId());
        long userId = loginUser.userId();
        List<FriendVO> records = jdbcTemplate.query("""
                        select f.id,
                               case when f.requester_id = ? then f.addressee_id else f.requester_id end as friend_id,
                               u.username,
                               f.status,
                               f.requested_at,
                               f.accepted_at
                        from user_friendships f
                        join users u on u.id = case when f.requester_id = ? then f.addressee_id else f.requester_id end
                        where f.status = 'ACCEPTED'
                          and (f.requester_id = ? or f.addressee_id = ?)
                        order by f.accepted_at desc, f.id desc
                        limit ? offset ?
                        """,
                (rs, rowNum) -> toFriend(rs),
                userId, userId, userId, userId, pageSize, offset);
        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total == null ? 0 : total));
    }

    @GetMapping("/api/me/friend-requests")
    public ApiResponse<PageResponse<FriendVO>> listIncomingRequests(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        return listRequests(loginUser.userId(), "addressee", page, pageSize);
    }

    @GetMapping("/api/me/friend-requests/sent")
    public ApiResponse<PageResponse<FriendVO>> listSentRequests(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        return listRequests(loginUser.userId(), "requester", page, pageSize);
    }

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/api/me/friend-requests")
    public ApiResponse<FriendVO> sendRequest(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody FriendRequest request
    ) {
        Long exists = jdbcTemplate.queryForObject("select count(*) from users where id = ?", Long.class, request.addresseeId());
        if (exists == null || exists == 0) {
            throw BusinessException.notFound("用户不存在");
        }
        if (loginUser.userId().equals(request.addresseeId())) {
            throw BusinessException.badRequest("不能添加自己为好友");
        }
        List<String> existing = jdbcTemplate.query("""
                        select status from user_friendships
                        where (requester_id = ? and addressee_id = ?)
                           or (requester_id = ? and addressee_id = ?)
                        """,
                (rs, rowNum) -> rs.getString("status"),
                loginUser.userId(), request.addresseeId(),
                request.addresseeId(), loginUser.userId());
        if (!existing.isEmpty()) {
            throw BusinessException.badRequest("已存在好友关系");
        }
        try {
            Long id = jdbcTemplate.queryForObject("""
                            insert into user_friendships (requester_id, addressee_id, status)
                            values (?, ?, 'PENDING')
                            returning id
                            """,
                    Long.class, loginUser.userId(), request.addresseeId());
            return ApiResponse.ok(getFriend(id, loginUser.userId()));
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.badRequest("好友请求已存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/me/friend-requests/{id}/accept")
    public ApiResponse<FriendVO> acceptRequest(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        int affected = jdbcTemplate.update("""
                        update user_friendships
                        set status = 'ACCEPTED', accepted_at = now(), updated_at = now()
                        where id = ? and addressee_id = ? and status = 'PENDING'
                        """,
                id, loginUser.userId());
        if (affected == 0) {
            throw BusinessException.notFound("好友请求不存在");
        }
        return ApiResponse.ok(getFriend(id, loginUser.userId()));
    }

    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/me/friend-requests/{id}/reject")
    public ApiResponse<FriendVO> rejectRequest(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        int affected = jdbcTemplate.update("""
                        update user_friendships
                        set status = 'REJECTED', updated_at = now()
                        where id = ? and addressee_id = ? and status = 'PENDING'
                        """,
                id, loginUser.userId());
        if (affected == 0) {
            throw BusinessException.notFound("好友请求不存在");
        }
        return ApiResponse.ok(getFriend(id, loginUser.userId()));
    }

    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping("/api/me/friends/{id}")
    public ApiResponse<Void> removeFriend(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        int affected = jdbcTemplate.update("""
                        delete from user_friendships
                        where id = ? and (requester_id = ? or addressee_id = ?)
                        """,
                id, loginUser.userId(), loginUser.userId());
        if (affected == 0) {
            throw BusinessException.notFound("好友关系不存在");
        }
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/me/friends/status")
    public ApiResponse<FriendshipStatusVO> checkStatus(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam @NotNull Long userId
    ) {
        String status = jdbcTemplate.query("""
                        select status from user_friendships
                        where ((requester_id = ? and addressee_id = ?)
                           or (requester_id = ? and addressee_id = ?))
                          and status != 'REJECTED'
                        order by updated_at desc
                        limit 1
                        """,
                (rs, rowNum) -> rs.getString("status"),
                loginUser.userId(), userId,
                userId, loginUser.userId())
                .stream().findFirst().orElse(null);
        return ApiResponse.ok(new FriendshipStatusVO(status));
    }

    private ApiResponse<PageResponse<FriendVO>> listRequests(Long userId, String role, long page, long pageSize) {
        long offset = (page - 1) * pageSize;
        String where = "addressee".equals(role)
                ? "f.addressee_id = ? and f.status = 'PENDING'"
                : "f.requester_id = ? and f.status = 'PENDING'";
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from user_friendships f where " + where,
                Long.class, userId);
        String otherAlias = "addressee".equals(role) ? "f.requester_id" : "f.addressee_id";
        List<FriendVO> records = jdbcTemplate.query("""
                        select f.id, %s as friend_id, u.username, f.status, f.requested_at, f.accepted_at
                        from user_friendships f
                        join users u on u.id = %s
                        where %s
                        order by f.requested_at desc, f.id desc
                        limit ? offset ?
                        """.formatted(otherAlias, otherAlias, where),
                (rs, rowNum) -> toFriend(rs),
                userId, pageSize, offset);
        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total == null ? 0 : total));
    }

    private FriendVO getFriend(Long friendshipId, Long currentUserId) {
        return jdbcTemplate.query("""
                        select f.id,
                               case when f.requester_id = ? then f.addressee_id else f.requester_id end as friend_id,
                               u.username,
                               f.status,
                               f.requested_at,
                               f.accepted_at
                        from user_friendships f
                        join users u on u.id = case when f.requester_id = ? then f.addressee_id else f.requester_id end
                        where f.id = ?
                        """,
                (rs, rowNum) -> toFriend(rs),
                currentUserId, currentUserId, friendshipId).stream().findFirst()
                .orElseThrow(() -> BusinessException.notFound("好友关系不存在"));
    }

    private FriendVO toFriend(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new FriendVO(
                rs.getLong("id"),
                rs.getLong("friend_id"),
                rs.getString("username"),
                rs.getString("status"),
                rs.getObject("requested_at", OffsetDateTime.class),
                rs.getObject("accepted_at", OffsetDateTime.class)
        );
    }

    public record FriendRequest(
            @NotNull Long addresseeId
    ) {
    }

    public record FriendVO(
            Long id,
            Long friendId,
            String username,
            String status,
            OffsetDateTime requestedAt,
            OffsetDateTime acceptedAt
    ) {
    }

    public record FriendshipStatusVO(
            String status
    ) {
    }
}
