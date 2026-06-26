package com.creatorspace.module.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 统一操作日志写入服务。
 */
@Service
public class OperationLogService {

    private final JdbcTemplate jdbcTemplate;

    public OperationLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(Long operatorId,
                       String operation,
                       String module,
                       String targetType,
                       Long targetId,
                       HttpServletRequest request,
                       Map<String, ?> detail) {
        try {
            jdbcTemplate.update("""
                            insert into operation_logs (
                                operator_id,
                                operation,
                                module,
                                target_type,
                                target_id,
                                request_method,
                                request_path,
                                ip_address,
                                user_agent,
                                detail_json
                            )
                            values (?, ?, ?, ?, ?, ?, ?, cast(? as inet), ?, cast(? as jsonb))
                            """,
                    operatorId,
                    operation,
                    module,
                    targetType,
                    targetId,
                    request == null ? null : request.getMethod(),
                    request == null ? null : request.getRequestURI(),
                    request == null ? null : request.getRemoteAddr(),
                    request == null ? null : request.getHeader("User-Agent"),
                    JsonSupport.toJson(detail));
        } catch (Exception ignored) {
            // 操作日志属于补充审计，不应影响主业务结果。
        }
    }

    public void record(Long operatorId,
                       String operation,
                       String module,
                       String targetType,
                       Long targetId,
                       Map<String, ?> detail) {
        record(operatorId, operation, module, targetType, targetId, null, detail);
    }
}
