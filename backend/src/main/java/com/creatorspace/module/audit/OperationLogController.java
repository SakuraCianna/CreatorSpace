package com.creatorspace.module.audit;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Validated
@RestController
public class OperationLogController {

    private final JdbcTemplate jdbcTemplate;

    public OperationLogController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/admin/operation-logs")
    public ApiResponse<PageResponse<OperationLogVO>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("where 1 = 1");
        if (module != null && !module.isBlank()) {
            where.append(" and module = ?");
            params.add(module.trim().toUpperCase());
        }
        if (operation != null && !operation.isBlank()) {
            where.append(" and operation like ?");
            params.add("%" + operation.trim() + "%");
        }
        if (operatorId != null) {
            where.append(" and operator_id = ?");
            params.add(operatorId);
        }
        if (startTime != null && !startTime.isBlank()) {
            where.append(" and created_at >= cast(? as timestamptz)");
            params.add(startTime.trim());
        }
        if (endTime != null && !endTime.isBlank()) {
            where.append(" and created_at <= cast(? as timestamptz)");
            params.add(endTime.trim());
        }

        Long total = jdbcTemplate.queryForObject("select count(*) from operation_logs " + where, Long.class, params.toArray());
        List<Object> listParams = new ArrayList<>(params);
        listParams.add(pageSize);
        listParams.add((page - 1) * pageSize);
        List<OperationLogVO> records = jdbcTemplate.query("""
                        select id,
                               operator_id,
                               operation,
                               module,
                               target_type,
                               target_id,
                               request_method,
                               request_path,
                               ip_address::text as ip_address,
                               user_agent,
                               detail_json::text as detail_json,
                               created_at
                        from operation_logs
                        %s
                        order by created_at desc, id desc
                        limit ? offset ?
                        """.formatted(where),
                (rs, rowNum) -> toLog(rs),
                listParams.toArray());
        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total == null ? 0 : total));
    }

    private OperationLogVO toLog(ResultSet rs) throws SQLException {
        return new OperationLogVO(
                rs.getLong("id"),
                rs.getObject("operator_id") == null ? null : rs.getLong("operator_id"),
                rs.getString("operation"),
                rs.getString("module"),
                rs.getString("target_type"),
                rs.getObject("target_id") == null ? null : rs.getLong("target_id"),
                rs.getString("request_method"),
                rs.getString("request_path"),
                rs.getString("ip_address"),
                rs.getString("user_agent"),
                rs.getString("detail_json"),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }

    public record OperationLogVO(
            Long id,
            Long operatorId,
            String operation,
            String module,
            String targetType,
            Long targetId,
            String requestMethod,
            String requestPath,
            String ipAddress,
            String userAgent,
            String detailJson,
            OffsetDateTime createdAt
    ) {
    }
}