package com.creatorspace.module.file;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 本地文件资源上传和后台资源列表接口。
 */
@Validated
@RestController
public class FileResourceController {

    private static final Set<String> ALLOWED_MODULES = Set.of("AVATAR", "COVER", "ARTICLE", "PROJECT", "INSPIRATION", "OTHER");
    private static final Map<String, UploadPolicy> ALLOWED_FILE_TYPES = Map.of(
            "image/webp", new UploadPolicy(".webp", Set.of(".webp")),
            "image/png", new UploadPolicy(".png", Set.of(".png")),
            "image/jpeg", new UploadPolicy(".jpg", Set.of(".jpg", ".jpeg")),
            "image/gif", new UploadPolicy(".gif", Set.of(".gif")),
            "application/pdf", new UploadPolicy(".pdf", Set.of(".pdf")),
            "text/plain", new UploadPolicy(".txt", Set.of(".txt")),
            "text/markdown", new UploadPolicy(".md", Set.of(".md", ".markdown"))
    );

    private final JdbcTemplate jdbcTemplate;
    private final Path storageRoot;
    private final String publicPrefix;

    public FileResourceController(
            JdbcTemplate jdbcTemplate,
            @Value("${app.storage.local-root:storage/uploads}") String localStorageRoot,
            @Value("${app.storage.public-prefix:/uploads}") String publicPrefix
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.storageRoot = resolveFromProjectRoot(localStorageRoot);
        this.publicPrefix = normalizePublicPrefix(publicPrefix);
    }

    // 登录用户上传文件资源，管理员和普通创作者共用同一套安全校验。
    @Transactional(rollbackFor = Exception.class)
    @PostMapping({"/api/admin/files/upload", "/api/creator/files/upload"})
    public ApiResponse<FileResourceVO> upload(
            HttpServletRequest request,
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "OTHER") String module
    ) {
        String normalizedModule = normalizeModule(module);
        String originalName = safeOriginalName(file.getOriginalFilename());
        ValidatedUpload validatedUpload = validateFile(file, originalName);
        if (isCreatorUpload(request) && !isImageContentType(validatedUpload.contentType())) {
            throw BusinessException.badRequest("普通用户只能上传图片资源");
        }
        String fileName = UUID.randomUUID() + validatedUpload.extension();
        LocalDate today = LocalDate.now();
        Path modulePath = Path.of(
                normalizedModule.toLowerCase(Locale.ROOT),
                String.valueOf(today.getYear()),
                "%02d".formatted(today.getMonthValue()),
                fileName
        );
        Path target = storageRoot.resolve(modulePath).normalize();
        if (!target.startsWith(storageRoot)) {
            throw BusinessException.badRequest("文件路径不合法");
        }
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException exception) {
            deleteStoredFile(target);
            throw BusinessException.badRequest("文件保存失败");
        }

        String relativePath = modulePath.toString().replace('\\', '/');
        String publicUrl = publicPrefix + "/" + relativePath;
        try {
            Long id = jdbcTemplate.queryForObject("""
                            insert into file_resources (
                                file_name,
                                original_name,
                                relative_path,
                                public_url,
                                file_type,
                                file_size,
                                storage_type,
                                module,
                                created_by
                            )
                            values (?, ?, ?, ?, ?, ?, 'LOCAL', ?, ?)
                            returning id
                            """,
                    Long.class,
                    fileName,
                    originalName,
                    relativePath,
                    publicUrl,
                    validatedUpload.contentType(),
                    file.getSize(),
                    normalizedModule,
                    loginUser.userId());
            return ApiResponse.ok(getFile(id));
        } catch (RuntimeException exception) {
            deleteStoredFile(target);
            throw exception;
        }
    }

    // 管理员查看文件资源列表。
    @GetMapping("/api/admin/files")
    public ApiResponse<PageResponse<FileResourceVO>> list(
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        return ApiResponse.ok(listResources(module, page, pageSize, null));
    }

    // 登录创作者查看自己上传的文件资源。
    @GetMapping("/api/creator/files")
    public ApiResponse<PageResponse<FileResourceVO>> listMine(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        return ApiResponse.ok(listResources(module, page, pageSize, loginUser.userId()));
    }

    private PageResponse<FileResourceVO> listResources(String module, long page, long pageSize, Long createdBy) {
        String normalizedModule = module == null || module.isBlank() ? "" : normalizeModule(module);
        List<Object> whereParams = new java.util.ArrayList<>();
        StringBuilder where = new StringBuilder("where 1 = 1");
        if (!normalizedModule.isEmpty()) {
            where.append(" and module = ?");
            whereParams.add(normalizedModule);
        }
        if (createdBy != null) {
            where.append(" and created_by = ?");
            whereParams.add(createdBy);
        }
        Object[] countParams = whereParams.toArray();
        Long total = jdbcTemplate.queryForObject("select count(*) from file_resources " + where, Long.class, countParams);

        List<Object> params = new java.util.ArrayList<>(whereParams);
        params.add(pageSize);
        params.add((page - 1) * pageSize);
        List<FileResourceVO> records = jdbcTemplate.query("""
                        select id,
                               file_name,
                               original_name,
                               relative_path,
                               public_url,
                               file_type,
                               file_size,
                               storage_type,
                               module,
                               created_at
                        from file_resources
                        %s
                        order by created_at desc, id desc
                        limit ? offset ?
                        """.formatted(where),
                (rs, rowNum) -> toFile(rs),
                params.toArray());
        return new PageResponse<>(records, page, pageSize, total == null ? 0 : total);
    }

    private FileResourceVO getFile(Long id) {
        return jdbcTemplate.query("""
                        select id,
                               file_name,
                               original_name,
                               relative_path,
                               public_url,
                               file_type,
                               file_size,
                               storage_type,
                               module,
                               created_at
                        from file_resources
                        where id = ?
                        """,
                (rs, rowNum) -> toFile(rs),
                id).stream().findFirst().orElseThrow(() -> BusinessException.notFound("文件资源不存在"));
    }

    private ValidatedUpload validateFile(MultipartFile file, String originalName) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择要上传的文件");
        }
        String contentType = normalizeContentType(file.getContentType());
        UploadPolicy policy = ALLOWED_FILE_TYPES.get(contentType);
        if (policy == null) {
            throw BusinessException.badRequest("文件类型不支持");
        }
        String originalExtension = extensionOf(originalName);
        if (!originalExtension.isEmpty() && !policy.allowedExtensions().contains(originalExtension)) {
            throw BusinessException.badRequest("文件扩展名与类型不匹配");
        }
        byte[] header = readHeader(file);
        if (!matchesContentSignature(contentType, header)) {
            throw BusinessException.badRequest("文件内容与类型不匹配");
        }
        return new ValidatedUpload(contentType, policy.canonicalExtension());
    }

    private String normalizeModule(String value) {
        String module = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_MODULES.contains(module)) {
            throw BusinessException.badRequest("文件模块不合法");
        }
        return module;
    }

    private String safeOriginalName(String value) {
        String original = value == null || value.isBlank() ? "upload" : value;
        String fileName = Path.of(original).getFileName().toString();
        return fileName.replaceAll("[^A-Za-z0-9._\\-\\u4e00-\\u9fa5]", "_");
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private byte[] readHeader(MultipartFile file) {
        try {
            return file.getInputStream().readNBytes(16);
        } catch (IOException exception) {
            throw BusinessException.badRequest("文件读取失败");
        }
    }

    private boolean matchesContentSignature(String contentType, byte[] header) {
        return switch (contentType) {
            case "image/png" -> startsWith(header, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "image/jpeg" -> startsWith(header, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "image/gif" -> startsWith(header, new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61})
                    || startsWith(header, new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61});
            case "image/webp" -> header.length >= 12
                    && startsWith(header, new byte[]{0x52, 0x49, 0x46, 0x46})
                    && header[8] == 0x57
                    && header[9] == 0x45
                    && header[10] == 0x42
                    && header[11] == 0x50;
            case "application/pdf" -> startsWith(header, new byte[]{0x25, 0x50, 0x44, 0x46});
            case "text/plain", "text/markdown" -> looksLikeText(header);
            default -> false;
        };
    }

    private boolean isCreatorUpload(HttpServletRequest request) {
        return request != null && request.getRequestURI() != null && request.getRequestURI().startsWith("/api/creator/");
    }

    private boolean isImageContentType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean looksLikeText(byte[] header) {
        for (byte value : header) {
            if (value == 0) {
                return false;
            }
        }
        return true;
    }

    private void deleteStoredFile(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // 数据库失败后的文件清理是补偿动作，不能覆盖原始异常。
        }
    }

    private Path resolveFromProjectRoot(String value) {
        Path configured = Path.of(value);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (cwd.getFileName() != null && "backend".equalsIgnoreCase(cwd.getFileName().toString())) {
            Path parent = cwd.getParent();
            if (parent != null) {
                return parent.resolve(configured).normalize();
            }
        }
        return cwd.resolve(configured).normalize();
    }

    private String normalizePublicPrefix(String value) {
        String prefix = value == null || value.isBlank() ? "/uploads" : value.trim();
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        return prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
    }

    private FileResourceVO toFile(ResultSet rs) throws SQLException {
        return new FileResourceVO(
                rs.getLong("id"),
                rs.getString("file_name"),
                rs.getString("original_name"),
                rs.getString("relative_path"),
                rs.getString("public_url"),
                rs.getString("file_type"),
                rs.getLong("file_size"),
                rs.getString("storage_type"),
                rs.getString("module"),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }

    public record FileResourceVO(
            Long id,
            String fileName,
            String originalName,
            String relativePath,
            String publicUrl,
            String fileType,
            Long fileSize,
            String storageType,
            String module,
            OffsetDateTime createdAt
    ) {
    }

    private record UploadPolicy(String canonicalExtension, Set<String> allowedExtensions) {
    }

    private record ValidatedUpload(String contentType, String extension) {
    }
}
