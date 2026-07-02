package com.creatorspace.module.file;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FileResourceControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadRejectsUnsupportedModuleAndEmptyFile() {
        FileResourceController controller = new FileResourceController(mock(JdbcTemplate.class), tempDir.toString(), "uploads");
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());

        BusinessException invalidModule = assertThrows(BusinessException.class,
                () -> controller.upload(request("/api/admin/files/upload"), user(), file, "bad"));
        assertEquals("文件模块不合法", invalidModule.getMessage());

        MockMultipartFile empty = new MockMultipartFile("file", "note.txt", "text/plain", new byte[0]);
        BusinessException emptyFile = assertThrows(BusinessException.class,
                () -> controller.upload(request("/api/admin/files/upload"), user(), empty, "OTHER"));
        assertEquals("请选择要上传的文件", emptyFile.getMessage());
    }

    @Test
    void creatorUploadRejectsNonMediaFile() {
        FileResourceController controller = new FileResourceController(mock(JdbcTemplate.class), tempDir.toString(), "/uploads");
        MockMultipartFile text = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.upload(request("/api/creator/files/upload"), user(), text, "ARTICLE"));

        assertEquals("普通用户只能上传图片和视频资源", exception.getMessage());
    }

    @Test
    void uploadRejectsExtensionAndSignatureMismatch() {
        FileResourceController controller = new FileResourceController(mock(JdbcTemplate.class), tempDir.toString(), "/uploads");

        MockMultipartFile mismatchExtension = new MockMultipartFile(
                "file", "image.txt", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        assertThrows(BusinessException.class,
                () -> controller.upload(request("/api/admin/files/upload"), user(), mismatchExtension, "COVER"));

        MockMultipartFile mismatchHeader = new MockMultipartFile(
                "file", "image.png", "image/png", "not really png".getBytes());
        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.upload(request("/api/admin/files/upload"), user(), mismatchHeader, "COVER"));
        assertEquals("文件内容与类型不匹配", exception.getMessage());
    }

    @Test
    void deleteRejectsReferencedFile() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        FileResourceController controller = new FileResourceController(jdbcTemplate, tempDir.toString(), "/uploads");
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(7L)))
                .thenAnswer(invocation -> List.of(new FileResourceController.FileResourceVO(
                        7L,
                        "file.png",
                        "file.png",
                        "cover/2026/07/file.png",
                        "/uploads/cover/2026/07/file.png",
                        "image/png",
                        8L,
                        "LOCAL",
                        "COVER",
                        java.time.OffsetDateTime.now()
                )));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(7L))).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> controller.delete(7L));

        assertEquals("文件资源已被内容引用，不能删除", exception.getMessage());
        verify(jdbcTemplate, never()).update(startsWith("delete from file_resources"), anyLong());
    }

    private HttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    private LoginUser user() {
        return new LoginUser(1L, "creator", List.of("USER"));
    }
}
