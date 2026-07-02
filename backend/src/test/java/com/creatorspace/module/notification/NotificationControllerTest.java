package com.creatorspace.module.notification;

import com.creatorspace.security.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationControllerTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final NotificationController controller = new NotificationController(jdbcTemplate);
    private final LoginUser loginUser = new LoginUser(8L, "reader", List.of("USER"));

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void listNotificationsReturnsPagedData() {
        when(jdbcTemplate.queryForObject("select count(*) from notifications where user_id = ?", Long.class, 8L)).thenReturn(1L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(8L), eq(20L), eq(0L))).thenAnswer(invocation -> {
            RowMapper mapper = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getLong("id")).thenReturn(1L);
            when(rs.getString("type")).thenReturn("COMMENT");
            when(rs.getString("title")).thenReturn("title");
            when(rs.getString("content")).thenReturn("content");
            when(rs.getString("target_type")).thenReturn("ARTICLE");
            when(rs.getObject("target_id", Long.class)).thenReturn(3L);
            when(rs.getObject("actor_id", Long.class)).thenReturn(4L);
            when(rs.getString("actor_name")).thenReturn("Alice");
            when(rs.getBoolean("is_read")).thenReturn(false);
            when(rs.getObject("created_at", OffsetDateTime.class)).thenReturn(OffsetDateTime.now());
            return List.of(mapper.mapRow(rs, 0));
        });

        var page = controller.listNotifications(loginUser, 1, 20).data();

        assertEquals(1, page.total());
        assertEquals("COMMENT", page.records().getFirst().type());
    }

    @Test
    void unreadAndMutationEndpointsUseCurrentUser() {
        when(jdbcTemplate.queryForObject("select count(*) from notifications where user_id = ? and is_read = false", Long.class, 8L)).thenReturn(5L);

        assertEquals(5L, controller.unreadCount(loginUser).data().count());

        controller.markRead(loginUser, 10L);
        controller.markAllRead(loginUser);
        controller.deleteNotification(loginUser, 11L);

        verify(jdbcTemplate).update("update notifications set is_read = true where id = ? and user_id = ?", 10L, 8L);
        verify(jdbcTemplate).update("update notifications set is_read = true where user_id = ? and is_read = false", 8L);
        verify(jdbcTemplate).update("delete from notifications where id = ? and user_id = ?", 11L, 8L);
    }

    @Test
    void pendingReviewAggregatesCounts() {
        when(jdbcTemplate.queryForObject("select count(*) from comments where status = 'PENDING'", Long.class)).thenReturn(1L);
        when(jdbcTemplate.queryForObject("select count(*) from guestbook_entries where status = 'PENDING'", Long.class)).thenReturn(2L);
        when(jdbcTemplate.queryForObject("select count(*) from articles where status = 'PENDING_REVIEW'", Long.class)).thenReturn(3L);
        when(jdbcTemplate.queryForObject("select count(*) from portfolio_projects where status = 'PENDING_REVIEW'", Long.class)).thenReturn(4L);

        var data = controller.pendingReview(new LoginUser(1L, "admin", List.of("ADMIN"))).data();

        assertEquals(1L, data.get("pendingComments"));
        assertEquals(10L, data.get("total"));
    }
}
