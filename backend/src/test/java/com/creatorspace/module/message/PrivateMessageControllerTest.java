package com.creatorspace.module.message;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.security.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrivateMessageControllerTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final PrivateMessageController controller = new PrivateMessageController(jdbcTemplate);
    private final LoginUser loginUser = new LoginUser(8L, "reader", List.of("USER"));

    @Test
    void sendMessageRejectsSelfMessage() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> controller.sendMessage(loginUser, new PrivateMessageController.SendMessageRequest(8L, "hello"))
        );

        assertEquals("不能给自己发送私信", exception.getMessage());
    }

    @Test
    void unreadCountUsesCurrentUser() {
        when(jdbcTemplate.queryForObject(
                "select count(*) from private_messages where receiver_id = ? and is_read = false",
                Long.class,
                8L
        )).thenReturn(3L);

        assertEquals(3L, controller.unreadCount(loginUser).data().count());
    }

    @Test
    void readConversationMarksOnlyCurrentReceiversMessages() {
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(org.springframework.jdbc.core.RowMapper.class),
                org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.eq(21L),
                org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.eq(8L)
        )).thenReturn(List.of(new PrivateMessageController.PrivateConversationVO(
                21L, 9L, "alice", "Alice", null, "hi", null, 9L, 1L
        )));

        controller.readConversation(loginUser, 21L);

        verify(jdbcTemplate).update("""
                update private_messages
                set is_read = true
                where conversation_id = ? and receiver_id = ? and is_read = false
                """, 21L, 8L);
    }
}
