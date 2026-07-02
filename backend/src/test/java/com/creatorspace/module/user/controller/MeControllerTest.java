package com.creatorspace.module.user.controller;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.security.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MeControllerTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final MeController controller = new MeController(jdbcTemplate);
    private final LoginUser loginUser = new LoginUser(9L, "creator", List.of("USER"));

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getMyProfileReturnsUserProfile() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(9L))).thenAnswer(invocation -> {
            RowMapper mapper = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject("id", Long.class)).thenReturn(9L);
            when(rs.getString("username")).thenReturn("creator");
            when(rs.getString("nickname")).thenReturn("Creator");
            when(rs.getString("avatar_url")).thenReturn("/avatar.png");
            when(rs.getString("bio")).thenReturn("bio");
            when(rs.getLong("article_count")).thenReturn(3L);
            when(rs.getLong("follower_count")).thenReturn(4L);
            when(rs.getLong("following_count")).thenReturn(5L);
            when(rs.getLong("friend_count")).thenReturn(2L);
            return List.of(mapper.mapRow(rs, 0));
        });

        var profile = controller.getMyProfile(loginUser).data();

        assertEquals(9L, profile.id());
        assertEquals("creator", profile.username());
        assertEquals(3L, profile.articleCount());
        assertEquals(2L, profile.friendCount());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void updateProfileWritesUserFieldsAndReloadsProfile() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(9L))).thenAnswer(invocation -> {
            RowMapper mapper = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject("id", Long.class)).thenReturn(9L);
            return List.of(mapper.mapRow(rs, 0));
        });

        controller.updateMyProfile(loginUser, new MeController.UpdateProfileRequest("Nick", "/a.png", "bio"));

        verify(jdbcTemplate).update(contains("update users set nickname"), eq("Nick"), eq("/a.png"), eq("bio"), eq(9L));
    }

    @Test
    void getMyProfileRejectsMissingUserAndPasswordUpdateValidatesOldPassword() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(9L))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> controller.getMyProfile(loginUser));

        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(jdbcTemplate.queryForObject("select password from users where id = ?", String.class, 9L)).thenReturn("encoded");
        when(encoder.matches("old", "encoded")).thenReturn(false);
        assertThrows(BusinessException.class,
                () -> controller.updateMyPassword(loginUser, new MeController.UpdatePasswordRequest("old", "new"), encoder));

        when(encoder.matches("old", "encoded")).thenReturn(true);
        when(encoder.encode("new")).thenReturn("new-encoded");
        controller.updateMyPassword(loginUser, new MeController.UpdatePasswordRequest("old", "new"), encoder);
        verify(jdbcTemplate).update(contains("update users set password"), eq("new-encoded"), eq(9L));
    }
}
