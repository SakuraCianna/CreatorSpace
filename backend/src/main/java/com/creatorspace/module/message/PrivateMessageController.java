package com.creatorspace.module.message;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.security.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@RestController
public class PrivateMessageController {

    private final JdbcTemplate jdbcTemplate;

    public PrivateMessageController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record PrivateConversationVO(
            Long id,
            Long peerId,
            String peerUsername,
            String peerNickname,
            String peerAvatarUrl,
            String lastMessageContent,
            OffsetDateTime lastMessageAt,
            Long lastSenderId,
            Long unreadCount
    ) {}

    public record PrivateMessageVO(
            Long id,
            Long conversationId,
            Long senderId,
            Long receiverId,
            String content,
            Boolean isRead,
            OffsetDateTime createdAt
    ) {}

    public record ConversationMessagesVO(
            PrivateConversationVO conversation,
            PageResponse<PrivateMessageVO> messages
    ) {}

    public record UnreadCountVO(long count) {}

    public record SendMessageRequest(
            @NotNull Long receiverId,
            @NotBlank @Size(max = 2000) String content
    ) {}

    @GetMapping("/api/me/messages/conversations")
    public ApiResponse<PageResponse<PrivateConversationVO>> listConversations(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        Long userId = loginUser.userId();
        Long total = jdbcTemplate.queryForObject("""
                select count(*)
                from message_conversations
                where participant_one_id = ? or participant_two_id = ?
                """, Long.class, userId, userId);

        List<PrivateConversationVO> records = jdbcTemplate.query(conversationSql("""
                        where c.participant_one_id = ? or c.participant_two_id = ?
                        order by c.last_message_at desc nulls last, c.updated_at desc, c.id desc
                        limit ? offset ?
                        """),
                conversationMapper(userId),
                userId, userId, userId, userId, userId, pageSize, (page - 1) * pageSize);

        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total == null ? 0 : total));
    }

    @GetMapping("/api/me/messages/conversations/{conversationId}")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<ConversationMessagesVO> getConversation(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) long pageSize
    ) {
        Long userId = loginUser.userId();
        PrivateConversationVO conversation = requireConversation(conversationId, userId);
        markConversationRead(conversationId, userId);

        Long total = jdbcTemplate.queryForObject(
                "select count(*) from private_messages where conversation_id = ?",
                Long.class,
                conversationId);
        List<PrivateMessageVO> messages = jdbcTemplate.query("""
                        select id, conversation_id, sender_id, receiver_id, content, is_read, created_at
                        from private_messages
                        where conversation_id = ?
                        order by created_at asc, id asc
                        limit ? offset ?
                        """,
                messageMapper(),
                conversationId, pageSize, (page - 1) * pageSize);

        return ApiResponse.ok(new ConversationMessagesVO(
                conversation,
                new PageResponse<>(messages, page, pageSize, total == null ? 0 : total)
        ));
    }

    @PostMapping("/api/me/messages")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<ConversationMessagesVO> sendMessage(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody SendMessageRequest request
    ) {
        Long senderId = loginUser.userId();
        Long receiverId = request.receiverId();
        if (Objects.equals(senderId, receiverId)) {
            throw BusinessException.badRequest("不能给自己发送私信");
        }
        ensureActiveUser(receiverId);

        Long conversationId = findOrCreateConversation(senderId, receiverId);
        String content = request.content().trim();
        Long messageId = insertMessage(conversationId, senderId, receiverId, content);
        jdbcTemplate.update("""
                update message_conversations
                set last_message_id = ?,
                    last_message_at = (select created_at from private_messages where id = ?),
                    updated_at = now()
                where id = ?
                """, messageId, messageId, conversationId);

        PrivateConversationVO conversation = requireConversation(conversationId, senderId);
        PrivateMessageVO message = requireMessage(messageId);
        return ApiResponse.ok(new ConversationMessagesVO(
                conversation,
                new PageResponse<>(List.of(message), 1, 1, 1)
        ));
    }

    @PutMapping("/api/me/messages/conversations/{conversationId}/read")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> readConversation(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long conversationId
    ) {
        requireConversation(conversationId, loginUser.userId());
        markConversationRead(conversationId, loginUser.userId());
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/me/messages/unread-count")
    public ApiResponse<UnreadCountVO> unreadCount(@AuthenticationPrincipal LoginUser loginUser) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from private_messages where receiver_id = ? and is_read = false",
                Long.class,
                loginUser.userId());
        return ApiResponse.ok(new UnreadCountVO(count == null ? 0 : count));
    }

    private PrivateConversationVO requireConversation(Long conversationId, Long userId) {
        List<PrivateConversationVO> result = jdbcTemplate.query(conversationSql("""
                        where c.id = ?
                          and (c.participant_one_id = ? or c.participant_two_id = ?)
                        """),
                conversationMapper(userId),
                userId, userId, userId, conversationId, userId, userId);
        if (result.isEmpty()) {
            throw BusinessException.notFound("私信会话不存在或无权访问");
        }
        return result.getFirst();
    }

    private PrivateMessageVO requireMessage(Long messageId) {
        List<PrivateMessageVO> result = jdbcTemplate.query("""
                        select id, conversation_id, sender_id, receiver_id, content, is_read, created_at
                        from private_messages
                        where id = ?
                        """,
                messageMapper(),
                messageId);
        if (result.isEmpty()) {
            throw BusinessException.notFound("私信不存在");
        }
        return result.getFirst();
    }

    private Long findOrCreateConversation(Long senderId, Long receiverId) {
        long first = Math.min(senderId, receiverId);
        long second = Math.max(senderId, receiverId);
        List<Long> existing = jdbcTemplate.query(
                "select id from message_conversations where participant_one_id = ? and participant_two_id = ?",
                (rs, rowNum) -> rs.getLong("id"),
                first, second);
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    insert into message_conversations (participant_one_id, participant_two_id)
                    values (?, ?)
                    """, new String[]{"id"});
            ps.setLong(1, first);
            ps.setLong(2, second);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw BusinessException.badRequest("私信会话创建失败");
        }
        return key.longValue();
    }

    private Long insertMessage(Long conversationId, Long senderId, Long receiverId, String content) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    insert into private_messages (conversation_id, sender_id, receiver_id, content)
                    values (?, ?, ?, ?)
                    """, new String[]{"id"});
            ps.setLong(1, conversationId);
            ps.setLong(2, senderId);
            ps.setLong(3, receiverId);
            ps.setString(4, content);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw BusinessException.badRequest("私信发送失败");
        }
        return key.longValue();
    }

    private void ensureActiveUser(Long userId) {
        List<Long> users = jdbcTemplate.query(
                "select id from users where id = ? and status = 'ACTIVE'",
                (rs, rowNum) -> rs.getLong("id"),
                userId);
        if (users.isEmpty()) {
            throw BusinessException.notFound("接收用户不存在或不可用");
        }
    }

    private void markConversationRead(Long conversationId, Long userId) {
        jdbcTemplate.update("""
                update private_messages
                set is_read = true
                where conversation_id = ? and receiver_id = ? and is_read = false
                """, conversationId, userId);
    }

    private String conversationSql(String tail) {
        return """
                select c.id,
                       case when c.participant_one_id = ? then c.participant_two_id else c.participant_one_id end as peer_id,
                       u.username as peer_username,
                       u.nickname as peer_nickname,
                       u.avatar_url as peer_avatar_url,
                       m.content as last_message_content,
                       c.last_message_at,
                       m.sender_id as last_sender_id,
                       (
                           select count(*)
                           from private_messages unread
                           where unread.conversation_id = c.id
                             and unread.receiver_id = ?
                             and unread.is_read = false
                       ) as unread_count
                from message_conversations c
                join users u on u.id = case when c.participant_one_id = ? then c.participant_two_id else c.participant_one_id end
                left join private_messages m on m.id = c.last_message_id
                """ + tail;
    }

    private RowMapper<PrivateConversationVO> conversationMapper(Long userId) {
        return (rs, rowNum) -> new PrivateConversationVO(
                rs.getLong("id"),
                rs.getLong("peer_id"),
                rs.getString("peer_username"),
                rs.getString("peer_nickname"),
                rs.getString("peer_avatar_url"),
                rs.getString("last_message_content"),
                rs.getObject("last_message_at", OffsetDateTime.class),
                rs.getObject("last_sender_id", Long.class),
                rs.getLong("unread_count")
        );
    }

    private RowMapper<PrivateMessageVO> messageMapper() {
        return (rs, rowNum) -> new PrivateMessageVO(
                rs.getLong("id"),
                rs.getLong("conversation_id"),
                rs.getLong("sender_id"),
                rs.getLong("receiver_id"),
                rs.getString("content"),
                rs.getBoolean("is_read"),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }
}
