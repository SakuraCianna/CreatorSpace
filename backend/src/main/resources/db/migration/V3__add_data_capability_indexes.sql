-- Add query-oriented indexes for the data capability module.
-- Existing V1 tables already provide the required base schema; this migration
-- only adds non-breaking indexes for search, audit, and dashboard queries.

create index if not exists idx_search_logs_keyword_created_at
    on search_logs(keyword, created_at desc);

create index if not exists idx_operation_logs_module_created_at
    on operation_logs(module, created_at desc);

create index if not exists idx_operation_logs_operator_created_at
    on operation_logs(operator_id, created_at desc);

create index if not exists idx_ai_agent_tasks_status_created_at
    on ai_agent_tasks(status, created_at desc);

create index if not exists idx_ai_suggestions_status_created_at
    on ai_suggestions(status, created_at desc);
