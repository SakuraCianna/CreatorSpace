-- 为本地开发和产品演示准备一批稳定的展示数据。
insert into users (username, nickname, password_hash, avatar_url, bio, status, created_at, updated_at, last_login_at)
values
    ('demo_creator', '林星野', '$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u', '/uploads/demo/avatar-lin.webp', '独立开发者，关注个人知识库、AI 工作流和创作者工具。', 'ACTIVE', now() - interval '90 days', now() - interval '2 days', now() - interval '6 hours'),
    ('demo_designer', '许南栀', '$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u', '/uploads/demo/avatar-xu.webp', '产品设计师，喜欢把复杂系统做得更温柔、更容易使用。', 'ACTIVE', now() - interval '86 days', now() - interval '1 day', now() - interval '4 hours'),
    ('demo_engineer', '周砚白', '$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u', '/uploads/demo/avatar-zhou.webp', '后端工程师，偏爱清晰的领域模型和可验证的系统边界。', 'ACTIVE', now() - interval '80 days', now() - interval '3 days', now() - interval '9 hours'),
    ('demo_curator', '闻夏', '$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u', '/uploads/demo/avatar-wen.webp', '内容策展人，持续收集技术写作、灵感卡片和作品案例。', 'ACTIVE', now() - interval '70 days', now() - interval '5 hours', now() - interval '1 hour'),
    ('demo_reader_01', '海盐拿铁', '$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u', '/uploads/demo/avatar-reader-01.webp', '经常阅读长文，也会收藏实用的项目拆解。', 'ACTIVE', now() - interval '50 days', now() - interval '7 hours', now() - interval '3 hours'),
    ('demo_reader_02', '晨间航线', '$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u', '/uploads/demo/avatar-reader-02.webp', '关注 Vue、Spring Boot 和个人自动化工具。', 'ACTIVE', now() - interval '42 days', now() - interval '9 hours', now() - interval '5 hours'),
    ('demo_reader_03', '蓝图收藏夹', '$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u', '/uploads/demo/avatar-reader-03.webp', '喜欢从别人的作品里学习信息架构和动效细节。', 'ACTIVE', now() - interval '35 days', now() - interval '1 day', now() - interval '12 hours'),
    ('demo_reader_04', '夜航笔记', '$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u', '/uploads/demo/avatar-reader-04.webp', '记录灵感片段，偶尔给项目提体验建议。', 'ACTIVE', now() - interval '28 days', now() - interval '2 days', now() - interval '18 hours'),
    ('demo_archived_user', '暂停账号样例', '$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u', null, '用于展示禁用账号状态。', 'DISABLED', now() - interval '20 days', now() - interval '10 days', null)
on conflict (username) do update
set nickname = excluded.nickname,
    avatar_url = excluded.avatar_url,
    bio = excluded.bio,
    status = excluded.status,
    updated_at = excluded.updated_at,
    last_login_at = excluded.last_login_at;

insert into user_roles (user_id, role_id)
select u.id, r.id
from users u
join roles r on r.code = 'USER'
where u.username like 'demo_%'
on conflict (user_id, role_id) do nothing;

insert into user_friendships (requester_id, addressee_id, status, requested_at, accepted_at, updated_at)
select requester.id,
       addressee.id,
       seed.status,
       now() - (seed.days_ago || ' days')::interval,
       case when seed.status = 'ACCEPTED' then now() - ((seed.days_ago - 1) || ' days')::interval else null end,
       now() - ((seed.days_ago - 1) || ' days')::interval
from (
    values
        ('demo_creator', 'demo_designer', 'ACCEPTED', 30),
        ('demo_creator', 'demo_engineer', 'ACCEPTED', 28),
        ('demo_creator', 'demo_curator', 'ACCEPTED', 25),
        ('demo_designer', 'demo_reader_01', 'ACCEPTED', 18),
        ('demo_engineer', 'demo_reader_02', 'ACCEPTED', 17),
        ('demo_curator', 'demo_reader_03', 'PENDING', 10),
        ('demo_reader_04', 'demo_creator', 'PENDING', 7),
        ('demo_archived_user', 'demo_reader_01', 'REJECTED', 6)
) as seed(requester_username, addressee_username, status, days_ago)
join users requester on requester.username = seed.requester_username
join users addressee on addressee.username = seed.addressee_username
on conflict (requester_id, addressee_id) do update
set status = excluded.status,
    accepted_at = excluded.accepted_at,
    updated_at = excluded.updated_at;

insert into categories (module, name, slug, description, sort_order, enabled)
values
    ('ARTICLE', 'AI 创作工作流', 'demo-article-ai-workflow', 'AI 辅助写作、图片生成和内容生产链路。', 10, true),
    ('ARTICLE', '工程札记', 'demo-article-engineering-notes', '后端、前端、数据库和工程质量复盘。', 20, true),
    ('ARTICLE', '产品设计', 'demo-article-product-design', '产品策略、信息架构、交互和视觉语言。', 30, true),
    ('ARTICLE', '个人知识库', 'demo-article-knowledge-base', '知识管理、笔记系统和长期主义写作。', 40, true),
    ('ARTICLE', '运营复盘', 'demo-article-operations', '站点增长、内容节奏和数据观察。', 50, true),
    ('ARTICLE', '幕后日志', 'demo-article-studio-log', 'CreatorSpace 建设过程里的细节记录。', 60, true),
    ('ARTICLE', '私密草稿', 'demo-article-private-draft', '用于展示草稿和私密内容，不进入公开列表。', 70, true),
    ('PROJECT', 'Web 应用', 'demo-project-web-app', '完整前后端应用、管理后台和内容系统。', 10, true),
    ('PROJECT', 'AI 工具', 'demo-project-ai-tool', '嵌入 AI 能力的创作者效率工具。', 20, true),
    ('PROJECT', '设计系统', 'demo-project-design-system', '组件规范、视觉 token 和交互动效。', 30, true),
    ('PROJECT', '自动化脚本', 'demo-project-automation', '把重复工作交给脚本和流水线。', 40, true),
    ('PROJECT', '实验原型', 'demo-project-prototype', '未完全产品化但能说明想法的探索。', 50, true),
    ('INSPIRATION', '界面灵感', 'demo-inspiration-interface', '用于主页和灵感墙的界面参考。', 10, true),
    ('INSPIRATION', '提示词', 'demo-inspiration-prompt', '适合收藏和复用的 Prompt 片段。', 20, true),
    ('INSPIRATION', '代码片段', 'demo-inspiration-code', '可复用的小型实现和配置。', 30, true),
    ('INSPIRATION', '阅读摘录', 'demo-inspiration-reading', '文章、书籍和访谈里的启发。', 40, true),
    ('INSPIRATION', '视觉板', 'demo-inspiration-moodboard', '色彩、布局和质感方向。', 50, true)
on conflict (module, slug) do update
set name = excluded.name,
    description = excluded.description,
    sort_order = excluded.sort_order,
    enabled = excluded.enabled,
    updated_at = now();

insert into tags (name, slug, color, weight)
values
    ('AI 工作流', 'demo-tag-ai-workflow', '#7c3aed', 98),
    ('Spring Boot', 'demo-tag-spring-boot', '#2563eb', 92),
    ('Vue 3', 'demo-tag-vue-three', '#16a34a', 91),
    ('TypeScript', 'demo-tag-typescript', '#0f766e', 89),
    ('PostgreSQL', 'demo-tag-postgresql', '#0ea5e9', 84),
    ('产品设计', 'demo-tag-product-design', '#db2777', 88),
    ('动效', 'demo-tag-motion', '#f97316', 76),
    ('知识管理', 'demo-tag-knowledge', '#9333ea', 82),
    ('内容策略', 'demo-tag-content-strategy', '#f59e0b', 79),
    ('可观测性', 'demo-tag-observability', '#64748b', 70),
    ('测试覆盖', 'demo-tag-testing', '#475569', 73),
    ('灵感卡', 'demo-tag-inspiration-card', '#06b6d4', 86),
    ('设计系统', 'demo-tag-design-system', '#ec4899', 81),
    ('自动化', 'demo-tag-automation', '#22c55e', 77),
    ('个人网站', 'demo-tag-personal-site', '#a855f7', 83),
    ('内容后台', 'demo-tag-cms', '#3b82f6', 80),
    ('性能优化', 'demo-tag-performance', '#ef4444', 69),
    ('安全边界', 'demo-tag-security', '#111827', 67),
    ('作品集', 'demo-tag-portfolio', '#14b8a6', 78),
    ('Prompt', 'demo-tag-prompt', '#8b5cf6', 85)
on conflict (slug) do update
set name = excluded.name,
    color = excluded.color,
    weight = excluded.weight,
    updated_at = now();

insert into articles (
    title, slug, summary, content_markdown, cover_url, category_id, status, privacy_type,
    view_count, like_count, comment_count, is_top, is_recommend, publish_time, created_by, updated_by,
    created_at, updated_at
)
select seed.title,
       seed.slug,
       seed.summary,
       seed.content_markdown,
       seed.cover_url,
       category.id,
       seed.status,
       seed.privacy_type,
       seed.view_count,
       seed.like_count,
       seed.comment_count,
       seed.is_top,
       seed.is_recommend,
       case when seed.status = 'PUBLISHED' then now() - (seed.publish_days_ago || ' days')::interval else null end,
       author.id,
       author.id,
       now() - ((seed.publish_days_ago + 3) || ' days')::interval,
       now() - (seed.publish_days_ago || ' days')::interval
from (
    values
        ('把个人站点做成创作中枢', 'demo-article-creator-hub', '从博客、作品集、灵感库到后台管理，整理 CreatorSpace 的核心产品结构。', '## 创作中枢\nCreatorSpace 的核心不是单页展示，而是把内容、作品、灵感和运营动作放进同一个可维护系统。', '/uploads/demo/article-creator-hub.webp', 'demo-article-product-design', 'PUBLISHED', 'PUBLIC', 2680, 142, 8, true, true, 2, 'demo_creator'),
        ('AI 辅助写作的五段式流程', 'demo-article-ai-writing-flow', '用选题、资料、草稿、校对和复盘拆开 AI 写作流程。', '## 五段式流程\n先让 AI 做结构，再由人补判断，最后用清单回收质量。', '/uploads/demo/article-ai-writing-flow.webp', 'demo-article-ai-workflow', 'PUBLISHED', 'PUBLIC', 2418, 131, 7, false, true, 4, 'demo_creator'),
        ('Vue 页面从 TSX 迁移到 SFC 的边界', 'demo-article-vue-sfc-boundary', '记录 Vue 3 + TypeScript 页面组织方式，以及什么时候该抽 composable。', '## SFC 边界\n页面承接交互，数据请求走服务，复用状态放进 composable。', '/uploads/demo/article-vue-sfc.webp', 'demo-article-engineering-notes', 'PUBLISHED', 'PUBLIC', 2190, 118, 5, false, true, 6, 'demo_engineer'),
        ('后台 CRUD 的第一阶段设计', 'demo-article-admin-crud-phase-one', '先把文章、作品、分类和标签做成稳定闭环，再谈高级运营能力。', '## 第一阶段\n稳定的列表、表单、状态流转和错误提示，比复杂大屏更早产生价值。', '/uploads/demo/article-admin-crud.webp', 'demo-article-engineering-notes', 'PUBLISHED', 'PUBLIC', 2033, 105, 6, false, true, 8, 'demo_engineer'),
        ('内容分类不该只是筛选器', 'demo-article-category-strategy', '分类承担信息架构职责，标签承担横向关联职责。', '## 分类策略\n分类需要少而稳定，标签可以多而轻。', '/uploads/demo/article-category-strategy.webp', 'demo-article-operations', 'PUBLISHED', 'PUBLIC', 1884, 94, 4, false, false, 11, 'demo_curator'),
        ('用 PostgreSQL 支撑创作者内容库', 'demo-article-postgres-content-library', '从全文检索、JSONB、扩展和迁移测试看内容库的基础设施。', '## 内容库\n结构化字段负责业务规则，JSONB 和扩展负责表达弹性。', '/uploads/demo/article-postgres.webp', 'demo-article-engineering-notes', 'PUBLISHED', 'PUBLIC', 1732, 88, 5, false, true, 13, 'demo_engineer'),
        ('灵感卡片如何避免变成垃圾抽屉', 'demo-article-inspiration-cards', '给灵感卡加来源、类型、颜色和标签，才能在未来被重新找到。', '## 灵感卡\n收藏的价值不在数量，而在之后能否被重新召回。', '/uploads/demo/article-inspiration-cards.webp', 'demo-article-knowledge-base', 'PUBLISHED', 'PUBLIC', 1640, 82, 3, false, false, 15, 'demo_curator'),
        ('产品主页动效的克制原则', 'demo-article-homepage-motion-budget', '让每个 section 只承担一种记忆点，减少动效之间互相争抢。', '## 动效预算\n动效应该帮用户理解节奏，而不是替内容抢镜头。', '/uploads/demo/article-motion-budget.webp', 'demo-article-product-design', 'PUBLISHED', 'PUBLIC', 1576, 80, 3, false, true, 17, 'demo_designer'),
        ('测试容器让数据库迁移更可信', 'demo-article-testcontainers-migration', '用真实 PostgreSQL 验证 Flyway、扩展、约束和初始化数据。', '## 迁移验证\n迁移脚本不是文本，它是要在真实数据库上被反复验证的契约。', '/uploads/demo/article-testcontainers.webp', 'demo-article-engineering-notes', 'PUBLISHED', 'PUBLIC', 1482, 76, 2, false, false, 20, 'demo_engineer'),
        ('个人知识库的公开与私密边界', 'demo-article-privacy-boundary', '公开文章负责表达，私密笔记负责积累，朋友可见负责协作。', '## 可见性\n同一套内容系统，需要同时支持公开表达和私密沉淀。', '/uploads/demo/article-privacy.webp', 'demo-article-knowledge-base', 'PUBLISHED', 'PUBLIC', 1390, 68, 2, false, false, 23, 'demo_creator'),
        ('从作品集看长期项目的叙事', 'demo-article-portfolio-storytelling', '作品不只是截图，还应该讲清问题、约束、取舍和结果。', '## 项目叙事\n好的作品页让读者理解你如何判断，而不是只看最终界面。', '/uploads/demo/article-portfolio.webp', 'demo-article-product-design', 'PUBLISHED', 'PUBLIC', 1286, 63, 1, false, false, 27, 'demo_designer'),
        ('如何给内容系统设计空状态', 'demo-article-empty-state-design', '空状态需要告诉用户下一步可以做什么，而不是只说没有数据。', '## 空状态\n空不是终点，它应该是下一次行动的入口。', '/uploads/demo/article-empty-state.webp', 'demo-article-product-design', 'PUBLISHED', 'PUBLIC', 1160, 51, 1, false, false, 31, 'demo_designer'),
        ('站点运营的轻量指标清单', 'demo-article-site-metrics', '访问路径、热门内容、收藏和评论足够支撑早期运营判断。', '## 指标清单\n第一阶段不需要大屏，需要能回答内容是否被看见。', '/uploads/demo/article-site-metrics.webp', 'demo-article-operations', 'PUBLISHED', 'PUBLIC', 980, 44, 1, false, false, 36, 'demo_curator'),
        ('自动化脚本如何服务内容生产', 'demo-article-content-automation', '用脚本处理重复任务，让创作者把注意力留给判断和表达。', '## 自动化\n自动化不是偷懒，它是把重复劳动从创作流程里拿出去。', '/uploads/demo/article-automation.webp', 'demo-article-ai-workflow', 'PUBLISHED', 'PUBLIC', 890, 38, 1, false, false, 42, 'demo_creator'),
        ('后台管理页的表格密度', 'demo-article-admin-table-density', '管理页应该服务扫描、比较和批量动作，而不是做成营销页。', '## 表格密度\n后台需要的是清晰、稳定、可重复使用。', '/uploads/demo/article-table-density.webp', 'demo-article-product-design', 'PUBLISHED', 'PUBLIC', 812, 35, 0, false, false, 49, 'demo_designer'),
        ('演示草稿：作品上传流程', 'demo-article-draft-upload-flow', '这篇内容用于展示草稿状态，不会出现在公开列表。', '## 草稿\n上传流程还在验证中。', null, 'demo-article-private-draft', 'DRAFT', 'PUBLIC', 0, 0, 0, false, false, 1, 'demo_creator'),
        ('朋友可见：季度复盘片段', 'demo-article-friends-quarter-review', '这篇内容用于展示朋友可见和可见名单能力。', '## 复盘\n这是一篇朋友范围内的阶段记录。', null, 'demo-article-private-draft', 'PRIVATE', 'SELECTED_FRIENDS', 42, 4, 0, false, false, 5, 'demo_creator'),
        ('归档样例：旧版首页说明', 'demo-article-archived-homepage-notes', '这篇内容用于展示归档状态。', '## 归档\n旧版首页说明已经不再公开展示。', null, 'demo-article-studio-log', 'ARCHIVED', 'PUBLIC', 120, 8, 0, false, false, 60, 'demo_creator')
) as seed(title, slug, summary, content_markdown, cover_url, category_slug, status, privacy_type, view_count, like_count, comment_count, is_top, is_recommend, publish_days_ago, author_username)
join categories category on category.slug = seed.category_slug
join users author on author.username = seed.author_username
on conflict (slug) do update
set title = excluded.title,
    summary = excluded.summary,
    content_markdown = excluded.content_markdown,
    cover_url = excluded.cover_url,
    category_id = excluded.category_id,
    status = excluded.status,
    privacy_type = excluded.privacy_type,
    view_count = excluded.view_count,
    like_count = excluded.like_count,
    comment_count = excluded.comment_count,
    is_top = excluded.is_top,
    is_recommend = excluded.is_recommend,
    publish_time = excluded.publish_time,
    updated_by = excluded.updated_by,
    updated_at = now();

insert into article_visibility_users (article_id, user_id, rule_type)
select article.id, visible_user.id, seed.rule_type
from (
    values
        ('demo-article-friends-quarter-review', 'demo_designer', 'ALLOW'),
        ('demo-article-friends-quarter-review', 'demo_engineer', 'ALLOW'),
        ('demo-article-friends-quarter-review', 'demo_curator', 'ALLOW')
) as seed(article_slug, username, rule_type)
join articles article on article.slug = seed.article_slug
join users visible_user on visible_user.username = seed.username
on conflict (article_id, user_id) do update
set rule_type = excluded.rule_type;

insert into article_versions (article_id, version_no, title, summary, content_markdown, created_by, created_at)
select article.id,
       seed.version_no,
       seed.title,
       seed.summary,
       seed.content_markdown,
       author.id,
       now() - (seed.days_ago || ' days')::interval
from (
    values
        ('demo-article-creator-hub', 1, '把个人站点做成创作中枢 - 初稿', '初稿版本。', '初稿记录了产品结构和信息架构。', 'demo_creator', 8),
        ('demo-article-creator-hub', 2, '把个人站点做成创作中枢', '发布版本。', '发布版本补充了后台管理和运营视角。', 'demo_creator', 2),
        ('demo-article-ai-writing-flow', 1, 'AI 辅助写作流程草案', '流程草案。', '草案先列出五个阶段。', 'demo_creator', 9),
        ('demo-article-ai-writing-flow', 2, 'AI 辅助写作的五段式流程', '发布版本。', '发布版本补充了校对和复盘。', 'demo_creator', 4),
        ('demo-article-admin-crud-phase-one', 1, '后台 CRUD 第一阶段提纲', '提纲版本。', '提纲先定义文章、作品、分类和标签。', 'demo_engineer', 12),
        ('demo-article-admin-crud-phase-one', 2, '后台 CRUD 的第一阶段设计', '发布版本。', '发布版本补充了状态和错误处理。', 'demo_engineer', 8),
        ('demo-article-draft-upload-flow', 1, '演示草稿：作品上传流程', '草稿版本。', '用于演示草稿不会公开展示。', 'demo_creator', 1)
) as seed(article_slug, version_no, title, summary, content_markdown, author_username, days_ago)
join articles article on article.slug = seed.article_slug
join users author on author.username = seed.author_username
on conflict (article_id, version_no) do update
set title = excluded.title,
    summary = excluded.summary,
    content_markdown = excluded.content_markdown;

insert into article_tags (article_id, tag_id)
select article.id, tag.id
from (
    values
        ('demo-article-creator-hub', 'demo-tag-personal-site'),
        ('demo-article-creator-hub', 'demo-tag-cms'),
        ('demo-article-creator-hub', 'demo-tag-product-design'),
        ('demo-article-ai-writing-flow', 'demo-tag-ai-workflow'),
        ('demo-article-ai-writing-flow', 'demo-tag-content-strategy'),
        ('demo-article-ai-writing-flow', 'demo-tag-prompt'),
        ('demo-article-vue-sfc-boundary', 'demo-tag-vue-three'),
        ('demo-article-vue-sfc-boundary', 'demo-tag-typescript'),
        ('demo-article-admin-crud-phase-one', 'demo-tag-spring-boot'),
        ('demo-article-admin-crud-phase-one', 'demo-tag-cms'),
        ('demo-article-category-strategy', 'demo-tag-content-strategy'),
        ('demo-article-postgres-content-library', 'demo-tag-postgresql'),
        ('demo-article-postgres-content-library', 'demo-tag-testing'),
        ('demo-article-inspiration-cards', 'demo-tag-inspiration-card'),
        ('demo-article-inspiration-cards', 'demo-tag-knowledge'),
        ('demo-article-homepage-motion-budget', 'demo-tag-motion'),
        ('demo-article-homepage-motion-budget', 'demo-tag-product-design'),
        ('demo-article-testcontainers-migration', 'demo-tag-testing'),
        ('demo-article-testcontainers-migration', 'demo-tag-postgresql'),
        ('demo-article-privacy-boundary', 'demo-tag-security'),
        ('demo-article-portfolio-storytelling', 'demo-tag-portfolio'),
        ('demo-article-empty-state-design', 'demo-tag-design-system'),
        ('demo-article-site-metrics', 'demo-tag-observability'),
        ('demo-article-content-automation', 'demo-tag-automation'),
        ('demo-article-admin-table-density', 'demo-tag-product-design'),
        ('demo-article-admin-table-density', 'demo-tag-cms')
) as seed(article_slug, tag_slug)
join articles article on article.slug = seed.article_slug
join tags tag on tag.slug = seed.tag_slug
on conflict (article_id, tag_id) do nothing;

insert into portfolio_projects (
    title, slug, description, cover_url, project_type, tech_stack, github_url, demo_url, video_url,
    content_markdown, status, is_recommend, sort_order, started_at, ended_at, created_by, updated_by, created_at, updated_at
)
select seed.title,
       seed.slug,
       seed.description,
       seed.cover_url,
       seed.project_type,
       seed.tech_stack::jsonb,
       seed.github_url,
       seed.demo_url,
       seed.video_url,
       seed.content_markdown,
       seed.status,
       seed.is_recommend,
       seed.sort_order,
       current_date - seed.started_days_ago,
       case when seed.ended_days_ago is null then null else current_date - seed.ended_days_ago end,
       author.id,
       author.id,
       now() - (seed.started_days_ago || ' days')::interval,
       now() - (seed.updated_days_ago || ' days')::interval
from (
    values
        ('CreatorSpace 内容后台', 'demo-project-content-console', '面向个人创作者的文章、作品、分类和标签管理后台。', '/uploads/demo/project-content-console.webp', 'WEB_APP', '["Vue 3", "TypeScript", "Spring Boot", "PostgreSQL"]', 'https://github.com/example/creatorspace-console', 'https://demo.creatorspace.local/console', null, '## 项目说明\n用最小闭环管理内容生产和公开展示。', 'VISIBLE', true, 10, 120, null, 3, 'demo_creator'),
        ('AI 选题工作台', 'demo-project-ai-topic-lab', '把灵感卡、关键词和文章草稿串起来的选题工具。', '/uploads/demo/project-ai-topic-lab.webp', 'AI_TOOL', '["Vue 3", "LLM", "Prompt", "PostgreSQL"]', 'https://github.com/example/topic-lab', 'https://demo.creatorspace.local/topic-lab', null, '## 项目说明\n让选题从零散想法变成可执行的内容计划。', 'VISIBLE', true, 20, 110, null, 5, 'demo_creator'),
        ('沉浸式主页原型', 'demo-project-immersive-homepage', '以 WebGL、滚动叙事和内容模块组成的个人站首页。', '/uploads/demo/project-immersive-homepage.webp', 'FRONTEND', '["Vue 3", "GSAP", "Three.js", "Lenis"]', 'https://github.com/example/immersive-homepage', 'https://demo.creatorspace.local/homepage', '/uploads/demo/video-homepage.mp4', '## 项目说明\n主页不是海报，而是创作系统的入口。', 'VISIBLE', true, 30, 100, null, 2, 'demo_designer'),
        ('灵感卡片采集器', 'demo-project-inspiration-collector', '一键收集图片、链接、Prompt 和代码片段。', '/uploads/demo/project-inspiration-collector.webp', 'BROWSER_EXTENSION', '["TypeScript", "WebExtension", "IndexedDB"]', 'https://github.com/example/inspiration-collector', null, null, '## 项目说明\n把灵感保存成可检索的结构化卡片。', 'VISIBLE', false, 40, 95, null, 8, 'demo_curator'),
        ('内容质量检查清单', 'demo-project-quality-checklist', '发布前检查标题、摘要、封面、标签和可见性。', '/uploads/demo/project-quality-checklist.webp', 'WEB_APP', '["Vue 3", "Spring Boot", "Validation"]', 'https://github.com/example/quality-checklist', 'https://demo.creatorspace.local/checklist', null, '## 项目说明\n用清单降低发布失误。', 'VISIBLE', false, 50, 88, null, 7, 'demo_engineer'),
        ('个人知识地图', 'demo-project-knowledge-map', '将文章、标签、项目和灵感卡连成知识网络。', '/uploads/demo/project-knowledge-map.webp', 'DATA_VIZ', '["D3", "PostgreSQL", "Graph"]', null, 'https://demo.creatorspace.local/knowledge-map', null, '## 项目说明\n用图谱发现内容之间的长期关系。', 'VISIBLE', false, 60, 76, null, 9, 'demo_curator'),
        ('上传资源管理器', 'demo-project-file-resource-manager', '追踪文件资源与文章、作品、站点配置之间的引用关系。', '/uploads/demo/project-file-resource-manager.webp', 'WEB_APP', '["Spring Boot", "Object Storage", "PostgreSQL"]', 'https://github.com/example/file-resource-manager', null, null, '## 项目说明\n上传不是结束，引用和清理才是长期维护的关键。', 'VISIBLE', false, 70, 68, null, 6, 'demo_engineer'),
        ('评论审核队列', 'demo-project-comment-review-queue', '集中处理待审核评论、敏感词命中和用户反馈。', '/uploads/demo/project-comment-review.webp', 'ADMIN_TOOL', '["Spring Security", "Moderation", "PostgreSQL"]', null, 'https://demo.creatorspace.local/comment-review', null, '## 项目说明\n让互动可控，而不是让后台被噪音淹没。', 'VISIBLE', false, 80, 60, null, 10, 'demo_engineer'),
        ('自动发布流水线', 'demo-project-publish-automation', '把构建检查、内容校验和发布动作放进一个流水线。', '/uploads/demo/project-publish-automation.webp', 'AUTOMATION', '["GitHub Actions", "PowerShell", "Maven", "Vite"]', 'https://github.com/example/publish-automation', null, null, '## 项目说明\n减少重复操作，保留人工确认。', 'VISIBLE', false, 90, 52, null, 4, 'demo_creator'),
        ('轻量访问分析', 'demo-project-visit-analytics', '从访问日志里提取内容热度、来源和设备分布。', '/uploads/demo/project-visit-analytics.webp', 'DASHBOARD', '["SQL", "Charts", "PostgreSQL"]', null, 'https://demo.creatorspace.local/analytics', null, '## 项目说明\n早期数据分析只需要回答几个关键问题。', 'VISIBLE', false, 100, 45, null, 11, 'demo_curator'),
        ('旧版作品墙', 'demo-project-legacy-gallery', '早期作品墙样例，保留为隐藏状态。', null, 'PROTOTYPE', '["HTML", "CSS"]', null, null, null, '## 项目说明\n旧版方案已经不再公开展示。', 'HIDDEN', false, 200, 200, 120, 30, 'demo_designer'),
        ('草稿：移动端灵感页', 'demo-project-mobile-inspiration-draft', '移动端灵感页的交互草稿。', null, 'PROTOTYPE', '["Vue 3", "Touch Interaction"]', null, null, null, '## 项目说明\n仍在探索移动端布局。', 'DRAFT', false, 210, 20, null, 1, 'demo_designer')
) as seed(title, slug, description, cover_url, project_type, tech_stack, github_url, demo_url, video_url, content_markdown, status, is_recommend, sort_order, started_days_ago, ended_days_ago, updated_days_ago, author_username)
join users author on author.username = seed.author_username
on conflict (slug) do update
set title = excluded.title,
    description = excluded.description,
    cover_url = excluded.cover_url,
    project_type = excluded.project_type,
    tech_stack = excluded.tech_stack,
    github_url = excluded.github_url,
    demo_url = excluded.demo_url,
    video_url = excluded.video_url,
    content_markdown = excluded.content_markdown,
    status = excluded.status,
    is_recommend = excluded.is_recommend,
    sort_order = excluded.sort_order,
    updated_by = excluded.updated_by,
    updated_at = now();

insert into project_tags (project_id, tag_id)
select project.id, tag.id
from (
    values
        ('demo-project-content-console', 'demo-tag-cms'),
        ('demo-project-content-console', 'demo-tag-vue-three'),
        ('demo-project-content-console', 'demo-tag-spring-boot'),
        ('demo-project-content-console', 'demo-tag-postgresql'),
        ('demo-project-ai-topic-lab', 'demo-tag-ai-workflow'),
        ('demo-project-ai-topic-lab', 'demo-tag-prompt'),
        ('demo-project-immersive-homepage', 'demo-tag-motion'),
        ('demo-project-immersive-homepage', 'demo-tag-personal-site'),
        ('demo-project-inspiration-collector', 'demo-tag-inspiration-card'),
        ('demo-project-quality-checklist', 'demo-tag-testing'),
        ('demo-project-knowledge-map', 'demo-tag-knowledge'),
        ('demo-project-file-resource-manager', 'demo-tag-security'),
        ('demo-project-comment-review-queue', 'demo-tag-security'),
        ('demo-project-publish-automation', 'demo-tag-automation'),
        ('demo-project-visit-analytics', 'demo-tag-observability'),
        ('demo-project-mobile-inspiration-draft', 'demo-tag-product-design')
) as seed(project_slug, tag_slug)
join portfolio_projects project on project.slug = seed.project_slug
join tags tag on tag.slug = seed.tag_slug
on conflict (project_id, tag_id) do nothing;

insert into project_images (project_id, image_url, caption, sort_order)
select project.id, seed.image_url, seed.caption, seed.sort_order
from (
    values
        ('demo-project-content-console', '/uploads/demo/project-content-console-01.webp', '内容列表和状态筛选', 10),
        ('demo-project-content-console', '/uploads/demo/project-content-console-02.webp', '文章编辑表单', 20),
        ('demo-project-ai-topic-lab', '/uploads/demo/project-ai-topic-lab-01.webp', '选题看板', 10),
        ('demo-project-ai-topic-lab', '/uploads/demo/project-ai-topic-lab-02.webp', 'Prompt 组合器', 20),
        ('demo-project-immersive-homepage', '/uploads/demo/project-homepage-01.webp', 'Hero WebGL 场景', 10),
        ('demo-project-immersive-homepage', '/uploads/demo/project-homepage-02.webp', '滚动叙事分区', 20),
        ('demo-project-inspiration-collector', '/uploads/demo/project-collector-01.webp', '网页采集面板', 10),
        ('demo-project-quality-checklist', '/uploads/demo/project-checklist-01.webp', '发布前检查清单', 10),
        ('demo-project-knowledge-map', '/uploads/demo/project-knowledge-map-01.webp', '知识节点网络', 10),
        ('demo-project-file-resource-manager', '/uploads/demo/project-file-manager-01.webp', '资源引用关系', 10),
        ('demo-project-comment-review-queue', '/uploads/demo/project-comment-review-01.webp', '评论审核队列', 10),
        ('demo-project-publish-automation', '/uploads/demo/project-automation-01.webp', '发布流水线状态', 10),
        ('demo-project-visit-analytics', '/uploads/demo/project-analytics-01.webp', '访问路径趋势', 10)
) as seed(project_slug, image_url, caption, sort_order)
join portfolio_projects project on project.slug = seed.project_slug;

insert into inspiration_cards (title, content, image_url, card_type, source_url, color, is_public, sort_order, created_by, created_at, updated_at)
select seed.title,
       seed.content,
       seed.image_url,
       seed.card_type,
       seed.source_url,
       seed.color,
       seed.is_public,
       seed.sort_order,
       author.id,
       now() - (seed.days_ago || ' days')::interval,
       now() - (seed.updated_days_ago || ' days')::interval
from (
    values
        ('后台列表的三层信息密度', '主信息用于识别，辅助信息用于判断，操作区只放当下最常用的动作。', null, 'TEXT', null, '#e0f2fe', true, 10, 18, 2, 'demo_designer'),
        ('Prompt：把想法拆成任务卡', '请把以下想法拆成目标、约束、输入、输出和验收标准，并给出最小可行步骤。', null, 'PROMPT', null, '#ede9fe', true, 20, 16, 3, 'demo_creator'),
        ('代码片段：生成目录 URI', 'Path.toUri() 返回文件 URI 后，需要保证静态资源目录以斜杠结尾。', null, 'CODE', null, '#dcfce7', true, 30, 12, 1, 'demo_engineer'),
        ('玻璃质感不是透明度', '真正有层次的玻璃效果来自背景、边框、高光和模糊的配合。', '/uploads/demo/inspiration-glass.webp', 'IMAGE', null, '#fce7f3', true, 40, 22, 6, 'demo_designer'),
        ('内容系统的核心实体', '用户、文章、作品、分类、标签、文件和互动记录构成第一阶段闭环。', null, 'TEXT', null, '#fef3c7', true, 50, 14, 4, 'demo_engineer'),
        ('阅读摘录：长期项目', '长期项目需要一个能持续容纳变化的结构，而不是一次性完成的页面。', null, 'LINK', 'https://example.com/long-running-projects', '#e2e8f0', true, 60, 21, 8, 'demo_curator'),
        ('配色板：冷光工作台', '深墨绿、极浅青、少量紫色和明亮白，用于管理工具的安静高对比界面。', '/uploads/demo/inspiration-palette.webp', 'IMAGE', null, '#ccfbf1', true, 70, 9, 2, 'demo_designer'),
        ('Prompt：文章摘要改写', '请把这段摘要改写成 80 字以内，保留具体收益，避免夸张营销语气。', null, 'PROMPT', null, '#fae8ff', true, 80, 8, 2, 'demo_creator'),
        ('代码片段：分页边界', 'page 从 1 开始，pageSize 限制最大值，错误参数应返回清晰的 400。', null, 'CODE', null, '#dbeafe', true, 90, 7, 1, 'demo_engineer'),
        ('链接：测试金字塔', '把单元测试、集成测试和端到端验证放在不同成本层级。', null, 'LINK', 'https://example.com/testing-pyramid', '#f1f5f9', true, 100, 6, 2, 'demo_curator'),
        ('灵感图：卡片瀑布流', '不同类型卡片用色条和图标区分，正文保持统一阅读节奏。', '/uploads/demo/inspiration-card-wall.webp', 'IMAGE', null, '#fee2e2', true, 110, 5, 1, 'demo_designer'),
        ('Prompt：代码审查视角', '请从正确性、边界条件、安全风险、性能和测试价值审查以下 diff。', null, 'PROMPT', null, '#e9d5ff', true, 120, 4, 1, 'demo_engineer'),
        ('私有灵感：下季度产品线索', '用于展示非公开灵感卡，不进入公开灵感墙。', null, 'TEXT', null, '#f8fafc', false, 130, 3, 1, 'demo_creator'),
        ('代码片段：避免原始类型告警', '优先查看依赖源码或本地编译结果，再决定是否可以使用泛型。', null, 'CODE', null, '#ecfeff', true, 140, 2, 1, 'demo_engineer')
) as seed(title, content, image_url, card_type, source_url, color, is_public, sort_order, days_ago, updated_days_ago, author_username)
join users author on author.username = seed.author_username;

insert into inspiration_tags (inspiration_id, tag_id)
select inspiration.id, tag.id
from (
    values
        ('后台列表的三层信息密度', 'demo-tag-product-design'),
        ('Prompt：把想法拆成任务卡', 'demo-tag-prompt'),
        ('Prompt：把想法拆成任务卡', 'demo-tag-ai-workflow'),
        ('代码片段：生成目录 URI', 'demo-tag-spring-boot'),
        ('玻璃质感不是透明度', 'demo-tag-design-system'),
        ('内容系统的核心实体', 'demo-tag-cms'),
        ('阅读摘录：长期项目', 'demo-tag-content-strategy'),
        ('配色板：冷光工作台', 'demo-tag-design-system'),
        ('Prompt：文章摘要改写', 'demo-tag-prompt'),
        ('代码片段：分页边界', 'demo-tag-testing'),
        ('链接：测试金字塔', 'demo-tag-testing'),
        ('灵感图：卡片瀑布流', 'demo-tag-inspiration-card'),
        ('Prompt：代码审查视角', 'demo-tag-prompt'),
        ('代码片段：避免原始类型告警', 'demo-tag-testing')
) as seed(inspiration_title, tag_slug)
join inspiration_cards inspiration on inspiration.title = seed.inspiration_title
join tags tag on tag.slug = seed.tag_slug
on conflict (inspiration_id, tag_id) do nothing;

insert into comments (target_type, target_id, user_id, content, status, depth, like_count, ip_address, user_agent, created_at, updated_at)
select seed.target_type,
       seed.target_id,
       comment_user.id,
       seed.content,
       'APPROVED',
       0,
       seed.like_count,
       seed.ip_address::inet,
       seed.user_agent,
       now() - (seed.days_ago || ' days')::interval,
       now() - (seed.days_ago || ' days')::interval
from (
    select 'ARTICLE'::varchar as target_type, article.id as target_id, 'demo_reader_01' as username, '展示评论：这篇创作中枢的拆解很适合做产品介绍页。' as content, 12 as like_count, '127.0.0.21' as ip_address, 'CreatorSpace Demo Browser' as user_agent, 2 as days_ago
    from articles article where article.slug = 'demo-article-creator-hub'
    union all
    select 'ARTICLE', article.id, 'demo_reader_02', '展示评论：AI 写作流程里把校对单独拆出来很有用。', 8, '127.0.0.22', 'CreatorSpace Demo Browser', 3
    from articles article where article.slug = 'demo-article-ai-writing-flow'
    union all
    select 'ARTICLE', article.id, 'demo_reader_03', '展示评论：SFC 边界这部分解决了我对目录组织的疑惑。', 7, '127.0.0.23', 'CreatorSpace Demo Browser', 4
    from articles article where article.slug = 'demo-article-vue-sfc-boundary'
    union all
    select 'ARTICLE', article.id, 'demo_reader_04', '展示评论：后台 CRUD 第一阶段确实应该先稳定基础能力。', 6, '127.0.0.24', 'CreatorSpace Demo Browser', 5
    from articles article where article.slug = 'demo-article-admin-crud-phase-one'
    union all
    select 'PROJECT', project.id, 'demo_reader_01', '展示评论：内容后台这个项目很适合继续补批量操作。', 9, '127.0.0.31', 'CreatorSpace Demo Browser', 2
    from portfolio_projects project where project.slug = 'demo-project-content-console'
    union all
    select 'PROJECT', project.id, 'demo_reader_02', '展示评论：AI 选题工作台如果能接灵感卡会很好用。', 5, '127.0.0.32', 'CreatorSpace Demo Browser', 3
    from portfolio_projects project where project.slug = 'demo-project-ai-topic-lab'
    union all
    select 'PROJECT', project.id, 'demo_reader_03', '展示评论：沉浸式主页的动效节奏很完整。', 11, '127.0.0.33', 'CreatorSpace Demo Browser', 4
    from portfolio_projects project where project.slug = 'demo-project-immersive-homepage'
    union all
    select 'MESSAGE', 1, 'demo_reader_04', '展示评论：希望后续能看到完整的后台原型。', 3, '127.0.0.41', 'CreatorSpace Demo Browser', 1
) as seed
join users comment_user on comment_user.username = seed.username;

update comments
set root_id = id
where root_id is null
  and parent_id is null
  and content like '展示评论：%'
  and user_id in (select id from users where username like 'demo_%');

insert into comments (target_type, target_id, parent_id, root_id, user_id, reply_to_user_id, content, status, depth, like_count, ip_address, user_agent, created_at, updated_at)
select root.target_type,
       root.target_id,
       root.id,
       root.root_id,
       reply_user.id,
       root.user_id,
       seed.reply_content,
       'APPROVED',
       1,
       seed.like_count,
       seed.ip_address::inet,
       'CreatorSpace Demo Browser',
       now() - (seed.hours_ago || ' hours')::interval,
       now() - (seed.hours_ago || ' hours')::interval
from (
    values
        ('展示评论：这篇创作中枢的拆解很适合做产品介绍页。', 'demo_creator', '回复：后续后台管理页也会沿用这个信息架构。', 4, '127.0.1.21', 18),
        ('展示评论：AI 写作流程里把校对单独拆出来很有用。', 'demo_creator', '回复：校对阶段会加上事实核查和语气统一。', 3, '127.0.1.22', 20),
        ('展示评论：SFC 边界这部分解决了我对目录组织的疑惑。', 'demo_engineer', '回复：目录会继续保持页面、服务和共享逻辑分层。', 2, '127.0.1.23', 22),
        ('展示评论：后台 CRUD 第一阶段确实应该先稳定基础能力。', 'demo_engineer', '回复：是的，复杂功能会等基础闭环稳了再做。', 2, '127.0.1.24', 24),
        ('展示评论：内容后台这个项目很适合继续补批量操作。', 'demo_designer', '回复：批量状态变更会在原型阶段一起考虑。', 3, '127.0.1.31', 16),
        ('展示评论：AI 选题工作台如果能接灵感卡会很好用。', 'demo_curator', '回复：这也是灵感卡片后续最重要的入口之一。', 2, '127.0.1.32', 15),
        ('展示评论：沉浸式主页的动效节奏很完整。', 'demo_designer', '回复：每个 section 只保留一个主要动效记忆点。', 5, '127.0.1.33', 12),
        ('展示评论：希望后续能看到完整的后台原型。', 'demo_creator', '回复：下一阶段会先做后台内容管理原型。', 1, '127.0.1.41', 10)
) as seed(root_content, reply_username, reply_content, like_count, ip_address, hours_ago)
join comments root on root.content = seed.root_content
    and root.user_id in (select id from users where username like 'demo_%')
join users reply_user on reply_user.username = seed.reply_username;

update comments root
set reply_count = replies.reply_count
from (
    select parent_id, count(*) as reply_count
    from comments
    where parent_id is not null
    group by parent_id
) replies
where root.id = replies.parent_id
  and root.content like '展示评论：%'
  and root.user_id in (select id from users where username like 'demo_%');

insert into sensitive_words (word, match_type, severity, enabled, created_by)
select seed.word, seed.match_type, seed.severity, seed.enabled, demo_user.id
from (
    values
        ('demo-站外引流', 'CONTAINS', 'REVIEW', true),
        ('demo-刷量服务', 'CONTAINS', 'REJECT', true),
        ('demo-恶意脚本', 'CONTAINS', 'MASK', true),
        ('demo-未经授权转载', 'CONTAINS', 'REVIEW', true),
        ('demo-违规推广', 'CONTAINS', 'REJECT', true),
        ('demo-测试屏蔽词', 'EXACT', 'MASK', false)
) as seed(word, match_type, severity, enabled)
join users demo_user on demo_user.username = 'demo_curator'
on conflict (word) do nothing;

insert into theme_configs (theme_name, display_name, primary_color, background_type, background_image, font_family, card_style, layout_type, is_active, config_json)
values
    ('demo-studio-daylight', 'Demo Studio Daylight', '#2563eb', 'solid', null, 'Inter, system-ui, sans-serif', 'flat', 'workspace', false, '{"density": "comfortable", "tone": "daylight"}'::jsonb),
    ('demo-editor-focus', 'Demo Editor Focus', '#0f766e', 'gradient', null, 'LXGW WenKai, Inter, sans-serif', 'outline', 'article', false, '{"density": "focused", "readingWidth": "760px"}'::jsonb),
    ('demo-gallery-night', 'Demo Gallery Night', '#a855f7', 'image', '/uploads/demo/theme-gallery-night.webp', 'Inter, system-ui, sans-serif', 'glass', 'gallery', false, '{"density": "immersive", "motion": "soft"}'::jsonb)
on conflict (theme_name) do nothing;

insert into site_configs (config_key, config_value, description)
values
    ('demo.site.identity', '{"name": "CreatorSpace Demo", "slogan": "把创作、作品和灵感放进同一个空间"}'::jsonb, '展示站点基础身份信息'),
    ('demo.site.navigation', '{"items": ["首页", "文章", "作品", "灵感", "关于"]}'::jsonb, '展示公开站点导航'),
    ('demo.home.featuredArticles', '{"slugs": ["demo-article-creator-hub", "demo-article-ai-writing-flow", "demo-article-admin-crud-phase-one"]}'::jsonb, '展示首页推荐文章'),
    ('demo.home.featuredProjects', '{"slugs": ["demo-project-content-console", "demo-project-ai-topic-lab", "demo-project-immersive-homepage"]}'::jsonb, '展示首页推荐作品'),
    ('demo.home.featuredInspirations', '{"limit": 8, "cardTypes": ["TEXT", "PROMPT", "IMAGE", "CODE"]}'::jsonb, '展示首页灵感卡配置'),
    ('demo.content.defaultVisibility', '{"article": "PUBLIC", "comment": "PENDING"}'::jsonb, '展示内容默认可见性配置'),
    ('demo.upload.policy', '{"maxSizeMb": 20, "allowedTypes": ["image/webp", "image/png", "image/jpeg"]}'::jsonb, '展示上传策略配置'),
    ('demo.analytics.dashboard', '{"enabled": true, "sampleWindowDays": 30}'::jsonb, '展示运营数据看板配置')
on conflict (config_key) do nothing;

insert into file_resources (file_name, original_name, relative_path, public_url, file_type, file_size, storage_type, module, created_by, created_at)
select seed.file_name,
       seed.original_name,
       seed.relative_path,
       seed.public_url,
       seed.file_type,
       seed.file_size,
       'LOCAL',
       seed.module,
       owner_user.id,
       now() - (seed.days_ago || ' days')::interval
from (
    values
        ('demo-avatar-lin.webp', '林星野头像.webp', 'demo/avatar-lin.webp', '/uploads/demo/avatar-lin.webp', 'image/webp', 51230, 'AVATAR', 'demo_creator', 58),
        ('demo-avatar-xu.webp', '许南栀头像.webp', 'demo/avatar-xu.webp', '/uploads/demo/avatar-xu.webp', 'image/webp', 49888, 'AVATAR', 'demo_designer', 57),
        ('demo-avatar-zhou.webp', '周砚白头像.webp', 'demo/avatar-zhou.webp', '/uploads/demo/avatar-zhou.webp', 'image/webp', 50120, 'AVATAR', 'demo_engineer', 56),
        ('demo-avatar-wen.webp', '闻夏头像.webp', 'demo/avatar-wen.webp', '/uploads/demo/avatar-wen.webp', 'image/webp', 47650, 'AVATAR', 'demo_curator', 55),
        ('demo-article-cover-creator-hub.webp', '创作中枢封面.webp', 'demo/article-creator-hub.webp', '/uploads/demo/article-creator-hub.webp', 'image/webp', 184220, 'COVER', 'demo_creator', 30),
        ('demo-article-cover-ai-writing.webp', 'AI 写作流程封面.webp', 'demo/article-ai-writing-flow.webp', '/uploads/demo/article-ai-writing-flow.webp', 'image/webp', 172300, 'COVER', 'demo_creator', 29),
        ('demo-article-cover-admin-crud.webp', '后台 CRUD 封面.webp', 'demo/article-admin-crud.webp', '/uploads/demo/article-admin-crud.webp', 'image/webp', 165920, 'COVER', 'demo_engineer', 28),
        ('demo-project-cover-console.webp', '内容后台封面.webp', 'demo/project-content-console.webp', '/uploads/demo/project-content-console.webp', 'image/webp', 210500, 'PROJECT', 'demo_creator', 27),
        ('demo-project-cover-topic-lab.webp', 'AI 选题工作台封面.webp', 'demo/project-ai-topic-lab.webp', '/uploads/demo/project-ai-topic-lab.webp', 'image/webp', 198430, 'PROJECT', 'demo_creator', 26),
        ('demo-project-cover-homepage.webp', '沉浸式主页封面.webp', 'demo/project-immersive-homepage.webp', '/uploads/demo/project-immersive-homepage.webp', 'image/webp', 224300, 'PROJECT', 'demo_designer', 25),
        ('demo-inspiration-glass.webp', '玻璃质感灵感图.webp', 'demo/inspiration-glass.webp', '/uploads/demo/inspiration-glass.webp', 'image/webp', 132800, 'INSPIRATION', 'demo_designer', 24),
        ('demo-inspiration-palette.webp', '冷光工作台配色.webp', 'demo/inspiration-palette.webp', '/uploads/demo/inspiration-palette.webp', 'image/webp', 126700, 'INSPIRATION', 'demo_designer', 23),
        ('demo-theme-gallery-night.webp', '夜间作品墙背景.webp', 'demo/theme-gallery-night.webp', '/uploads/demo/theme-gallery-night.webp', 'image/webp', 245760, 'OTHER', 'demo_designer', 22)
) as seed(file_name, original_name, relative_path, public_url, file_type, file_size, module, owner_username, days_ago)
join users owner_user on owner_user.username = seed.owner_username;

insert into file_resource_references (file_id, target_type, target_id, usage_type)
select file.id, seed.target_type, seed.target_id, seed.usage_type
from (
    select 'demo-avatar-lin.webp' as file_name, 'USER' as target_type, demo_user.id as target_id, 'AVATAR' as usage_type from users demo_user where demo_user.username = 'demo_creator'
    union all
    select 'demo-avatar-xu.webp', 'USER', demo_user.id, 'AVATAR' from users demo_user where demo_user.username = 'demo_designer'
    union all
    select 'demo-avatar-zhou.webp', 'USER', demo_user.id, 'AVATAR' from users demo_user where demo_user.username = 'demo_engineer'
    union all
    select 'demo-avatar-wen.webp', 'USER', demo_user.id, 'AVATAR' from users demo_user where demo_user.username = 'demo_curator'
    union all
    select 'demo-article-cover-creator-hub.webp', 'ARTICLE', article.id, 'COVER' from articles article where article.slug = 'demo-article-creator-hub'
    union all
    select 'demo-article-cover-ai-writing.webp', 'ARTICLE', article.id, 'COVER' from articles article where article.slug = 'demo-article-ai-writing-flow'
    union all
    select 'demo-article-cover-admin-crud.webp', 'ARTICLE', article.id, 'COVER' from articles article where article.slug = 'demo-article-admin-crud-phase-one'
    union all
    select 'demo-project-cover-console.webp', 'PROJECT', project.id, 'COVER' from portfolio_projects project where project.slug = 'demo-project-content-console'
    union all
    select 'demo-project-cover-topic-lab.webp', 'PROJECT', project.id, 'COVER' from portfolio_projects project where project.slug = 'demo-project-ai-topic-lab'
    union all
    select 'demo-project-cover-homepage.webp', 'PROJECT', project.id, 'COVER' from portfolio_projects project where project.slug = 'demo-project-immersive-homepage'
    union all
    select 'demo-inspiration-glass.webp', 'INSPIRATION', inspiration.id, 'COVER' from inspiration_cards inspiration where inspiration.title = '玻璃质感不是透明度'
    union all
    select 'demo-theme-gallery-night.webp', 'THEME', theme.id, 'CONFIG' from theme_configs theme where theme.theme_name = 'demo-gallery-night'
) as seed(file_name, target_type, target_id, usage_type)
join file_resources file on file.file_name = seed.file_name
on conflict (file_id, target_type, target_id, usage_type) do nothing;

insert into visit_logs (path, target_type, target_id, ip_address, user_agent, referer, device_type, browser, operating_system, created_at)
select case
           when series.value % 5 = 0 then '/projects/demo-project-content-console'
           when series.value % 5 = 1 then '/articles/demo-article-creator-hub'
           when series.value % 5 = 2 then '/articles/demo-article-ai-writing-flow'
           when series.value % 5 = 3 then '/inspirations'
           else '/'
       end,
       case
           when series.value % 5 = 0 then 'PROJECT'
           when series.value % 5 in (1, 2) then 'ARTICLE'
           when series.value % 5 = 3 then 'INSPIRATION'
           else null
       end,
       case
           when series.value % 5 = 0 then (select id from portfolio_projects where slug = 'demo-project-content-console')
           when series.value % 5 = 1 then (select id from articles where slug = 'demo-article-creator-hub')
           when series.value % 5 = 2 then (select id from articles where slug = 'demo-article-ai-writing-flow')
           when series.value % 5 = 3 then (select id from inspiration_cards where title = '后台列表的三层信息密度')
           else null
       end,
       ('10.0.0.' || ((series.value % 200) + 10))::inet,
       'CreatorSpace Demo Browser',
       case when series.value % 4 = 0 then 'https://search.example/demo' else 'https://creatorspace.local' end,
       case when series.value % 3 = 0 then 'mobile' when series.value % 3 = 1 then 'desktop' else 'tablet' end,
       case when series.value % 2 = 0 then 'Chrome' else 'Edge' end,
       case when series.value % 3 = 0 then 'Android' when series.value % 3 = 1 then 'Windows' else 'iOS' end,
       now() - (series.value || ' hours')::interval
from generate_series(1, 80) as series(value);

insert into like_records (target_type, target_id, user_id, created_at)
select target.target_type,
       target.target_id,
       demo_user.id,
       now() - ((row_number() over (order by target.target_type, target.target_id, demo_user.id)) || ' hours')::interval
from (
    select 'ARTICLE' as target_type, id as target_id
    from articles
    where slug in ('demo-article-creator-hub', 'demo-article-ai-writing-flow', 'demo-article-admin-crud-phase-one', 'demo-article-homepage-motion-budget', 'demo-article-postgres-content-library')
    union all
    select 'PROJECT', id
    from portfolio_projects
    where slug in ('demo-project-content-console', 'demo-project-ai-topic-lab', 'demo-project-immersive-homepage')
    union all
    select 'INSPIRATION', id
    from inspiration_cards
    where title in ('后台列表的三层信息密度', 'Prompt：把想法拆成任务卡', '玻璃质感不是透明度')
) target
join users demo_user on demo_user.username in ('demo_reader_01', 'demo_reader_02', 'demo_reader_03', 'demo_reader_04', 'demo_curator')
where (target.target_id + demo_user.id) % 2 = 0
on conflict (target_type, target_id, user_id) do nothing;

insert into favorite_records (target_type, target_id, user_id, created_at)
select target.target_type,
       target.target_id,
       demo_user.id,
       now() - ((row_number() over (order by target.target_type, target.target_id, demo_user.id)) || ' days')::interval
from (
    select 'ARTICLE' as target_type, id as target_id
    from articles
    where slug in ('demo-article-creator-hub', 'demo-article-vue-sfc-boundary', 'demo-article-testcontainers-migration', 'demo-article-portfolio-storytelling')
    union all
    select 'PROJECT', id
    from portfolio_projects
    where slug in ('demo-project-content-console', 'demo-project-inspiration-collector', 'demo-project-knowledge-map')
    union all
    select 'INSPIRATION', id
    from inspiration_cards
    where title in ('代码片段：生成目录 URI', 'Prompt：文章摘要改写', '灵感图：卡片瀑布流')
) target
join users demo_user on demo_user.username in ('demo_reader_01', 'demo_reader_02', 'demo_reader_03', 'demo_reader_04')
where (target.target_id + demo_user.id) % 3 <> 0
on conflict (target_type, target_id, user_id) do nothing;

insert into friend_links (site_name, site_url, logo_url, description, status, sort_order)
values
    ('Open Design Notes', 'https://example.com/open-design-notes', '/uploads/demo/link-open-design.webp', '产品设计和工程协作笔记。', 'APPROVED', 10),
    ('Frontend Garden', 'https://example.com/frontend-garden', '/uploads/demo/link-frontend.webp', '前端交互、动效和组件实践。', 'APPROVED', 20),
    ('Backend Fieldbook', 'https://example.com/backend-fieldbook', '/uploads/demo/link-backend.webp', '后端架构、测试和数据库经验。', 'APPROVED', 30),
    ('Prompt Studio', 'https://example.com/prompt-studio', '/uploads/demo/link-prompt.webp', 'AI 工作流与 Prompt 模板。', 'APPROVED', 40),
    ('Indie Maker Logs', 'https://example.com/indie-maker-logs', null, '独立开发者的产品日志。', 'APPROVED', 50),
    ('Pending Partner', 'https://example.com/pending-partner', null, '用于展示待审核友链状态。', 'PENDING', 90),
    ('Hidden Archive', 'https://example.com/hidden-archive', null, '用于展示隐藏友链状态。', 'HIDDEN', 100);

insert into operation_logs (operator_id, operation, module, target_type, target_id, request_method, request_path, ip_address, user_agent, detail_json, created_at)
select demo_operator.id,
       case
           when series.value % 5 = 0 then '发布文章'
           when series.value % 5 = 1 then '更新作品'
           when series.value % 5 = 2 then '审核评论'
           when series.value % 5 = 3 then '调整站点配置'
           else '上传展示资源'
       end,
       case
           when series.value % 5 = 0 then 'ARTICLE'
           when series.value % 5 = 1 then 'PROJECT'
           when series.value % 5 = 2 then 'COMMENT'
           when series.value % 5 = 3 then 'SITE_CONFIG'
           else 'FILE'
       end,
       case
           when series.value % 5 = 0 then 'ARTICLE'
           when series.value % 5 = 1 then 'PROJECT'
           when series.value % 5 = 2 then 'COMMENT'
           when series.value % 5 = 3 then 'SITE_CONFIG'
           else 'FILE'
       end,
       case
           when series.value % 5 = 0 then (select id from articles where slug = 'demo-article-creator-hub')
           when series.value % 5 = 1 then (select id from portfolio_projects where slug = 'demo-project-content-console')
           when series.value % 5 = 2 then (select id from comments where content = '展示评论：这篇创作中枢的拆解很适合做产品介绍页。')
           when series.value % 5 = 3 then (select id from site_configs where config_key = 'demo.site.identity')
           else (select id from file_resources where file_name = 'demo-project-cover-console.webp')
       end,
       case when series.value % 3 = 0 then 'POST' when series.value % 3 = 1 then 'PUT' else 'PATCH' end,
       '/api/admin/demo/' || series.value,
       ('10.1.0.' || ((series.value % 120) + 20))::inet,
       'CreatorSpace Admin Browser',
       jsonb_build_object('seed', true, 'batch', 'V2', 'sequence', series.value),
       now() - (series.value || ' hours')::interval
from generate_series(1, 36) as series(value)
join users demo_operator on demo_operator.username = 'demo_engineer';

insert into user_profiles (user_id, display_name, headline, location, website_url, profile_json)
select id,
       nickname,
       'CreatorSpace 展示用户',
       'Shanghai',
       'https://example.com/demo-profile',
       jsonb_build_object('demo', true, 'interests', array['writing', 'portfolio', 'cms'])
from users
where username in ('demo_creator', 'demo_designer', 'demo_engineer', 'demo_curator')
on conflict (user_id) do update
set display_name = excluded.display_name,
    headline = excluded.headline,
    location = excluded.location,
    website_url = excluded.website_url,
    profile_json = excluded.profile_json,
    updated_at = now();

insert into site_profiles (profile_key, display_name, headline, avatar_url, bio, contact_email, location, profile_json, is_active)
values (
    'demo-owner',
    '林星野',
    '把个人博客、作品展厅和灵感写作桌放进同一个空间',
    '/uploads/demo/avatar-lin.webp',
    '独立开发者，长期整理内容系统、AI 工作流和创作者工具。',
    'demo@example.com',
    'Shanghai',
    '{"tone": "warm technical", "focus": ["writing", "portfolio", "creative system"]}'::jsonb,
    true
)
on conflict (profile_key) do update
set display_name = excluded.display_name,
    headline = excluded.headline,
    avatar_url = excluded.avatar_url,
    bio = excluded.bio,
    contact_email = excluded.contact_email,
    location = excluded.location,
    profile_json = excluded.profile_json,
    is_active = excluded.is_active,
    updated_at = now();

insert into navigation_items (label, path, icon, group_name, sort_order, visible, extra_json)
values
    ('首页', '/', 'home', 'primary', 10, true, '{}'::jsonb),
    ('文章', '/articles', 'book-open', 'primary', 20, true, '{}'::jsonb),
    ('作品', '/projects', 'images', 'primary', 30, true, '{}'::jsonb),
    ('灵感', '/inspirations', 'lightbulb', 'primary', 40, true, '{}'::jsonb),
    ('关于', '/about', 'info', 'primary', 50, true, '{}'::jsonb);

insert into social_links (platform, label, url, icon, sort_order, visible)
values
    ('github', 'GitHub', 'https://github.com/SakuraCianna/CreatorSpace', 'github', 10, true),
    ('mail', 'Mail', 'mailto:demo@example.com', 'mail', 20, true),
    ('portfolio', 'Portfolio', 'https://example.com/creatorspace', 'sparkles', 30, true);

insert into page_configs (page_key, title, slug, seo_title, seo_description, content_json, layout_json, status)
values
    ('home', 'CreatorSpace 首页', 'home', 'CreatorSpace - 个人主题空间', '个人主题风格博客与创意作品展示平台', '{"modules": ["hero", "articles", "projects", "inspirations", "themes"]}'::jsonb, '{"density": "immersive"}'::jsonb, 'PUBLISHED'),
    ('about', '关于创作者', 'about', '关于 CreatorSpace', '了解站点主人、技能和创作方向', '{"sections": ["bio", "skills", "contact"]}'::jsonb, '{"density": "editorial"}'::jsonb, 'PUBLISHED'),
    ('search', '站内搜索', 'search', 'CreatorSpace 搜索', '搜索文章、作品和灵感卡片', '{"scope": ["articles", "projects", "inspirations"]}'::jsonb, '{"density": "utility"}'::jsonb, 'PUBLISHED')
on conflict (page_key) do update
set title = excluded.title,
    seo_title = excluded.seo_title,
    seo_description = excluded.seo_description,
    content_json = excluded.content_json,
    layout_json = excluded.layout_json,
    status = excluded.status,
    updated_at = now();

insert into theme_variants (theme_id, variant_name, density, color_tokens, font_tokens, motion_tokens, card_tokens)
select theme.id,
       seed.variant_name,
       seed.density,
       seed.color_tokens::jsonb,
       seed.font_tokens::jsonb,
       seed.motion_tokens::jsonb,
       seed.card_tokens::jsonb
from (
    values
        ('glass-space', 'Default Gallery', 'comfortable', '{"primary": "#7c3aed", "accent": "#54e6c8"}', '{"display": "Space Grotesk"}', '{"level": "rich"}', '{"radius": 8, "style": "glass"}'),
        ('demo-editor-focus', 'Reading Focus', 'compact', '{"primary": "#0f766e", "accent": "#f59e0b"}', '{"display": "Newsreader"}', '{"level": "calm"}', '{"radius": 8, "style": "outline"}')
) as seed(theme_name, variant_name, density, color_tokens, font_tokens, motion_tokens, card_tokens)
join theme_configs theme on theme.theme_name = seed.theme_name
on conflict (theme_id, variant_name) do update
set density = excluded.density,
    color_tokens = excluded.color_tokens,
    font_tokens = excluded.font_tokens,
    motion_tokens = excluded.motion_tokens,
    card_tokens = excluded.card_tokens,
    updated_at = now();

insert into theme_assets (theme_id, file_id, asset_type, asset_url, sort_order)
select theme.id, file.id, 'BACKGROUND', file.public_url, 10
from theme_configs theme
join file_resources file on file.file_name = 'demo-theme-gallery-night.webp'
where theme.theme_name = 'demo-gallery-night';

insert into theme_versions (theme_id, version_no, snapshot_json, created_by)
select theme.id,
       1,
       jsonb_build_object('themeName', theme.theme_name, 'primaryColor', theme.primary_color, 'seed', true),
       demo_user.id
from theme_configs theme
join users demo_user on demo_user.username = 'demo_designer'
where theme.theme_name in ('glass-space', 'demo-gallery-night')
on conflict (theme_id, version_no) do update
set snapshot_json = excluded.snapshot_json;

insert into tag_aliases (tag_id, alias)
select tag.id, seed.alias
from (
    values
        ('demo-tag-ai-workflow', 'AI 创作'),
        ('demo-tag-product-design', '产品体验'),
        ('demo-tag-motion', '页面动效'),
        ('demo-tag-cms', '内容工作台')
) as seed(slug, alias)
join tags tag on tag.slug = seed.slug
on conflict (tag_id, alias) do nothing;

insert into article_series (title, slug, description, cover_url, sort_order, status, created_by)
select seed.title,
       seed.slug,
       seed.description,
       seed.cover_url,
       seed.sort_order,
       'ACTIVE',
       demo_user.id
from (
    values
        ('个人主题空间建设手记', 'demo-series-personal-theme-space', '从首页气质到后台工作台的完整建设记录。', '/uploads/demo/article-creator-hub.webp', 10),
        ('内容系统工程札记', 'demo-series-content-system-engineering', '数据库、接口、迁移和后台能力的工程记录。', '/uploads/demo/article-postgres.webp', 20)
) as seed(title, slug, description, cover_url, sort_order)
join users demo_user on demo_user.username = 'demo_creator'
on conflict (slug) do update
set title = excluded.title,
    description = excluded.description,
    cover_url = excluded.cover_url,
    sort_order = excluded.sort_order,
    updated_at = now();

insert into article_series_items (series_id, article_id, sort_order, note)
select series.id, article.id, seed.sort_order, seed.note
from (
    values
        ('demo-series-personal-theme-space', 'demo-article-creator-hub', 10, '系列入口文章'),
        ('demo-series-personal-theme-space', 'demo-article-homepage-motion-budget', 20, '动效预算说明'),
        ('demo-series-content-system-engineering', 'demo-article-postgres-content-library', 10, '数据库基础设施'),
        ('demo-series-content-system-engineering', 'demo-article-testcontainers-migration', 20, '迁移测试验证')
) as seed(series_slug, article_slug, sort_order, note)
join article_series series on series.slug = seed.series_slug
join articles article on article.slug = seed.article_slug
on conflict (series_id, article_id) do update
set sort_order = excluded.sort_order,
    note = excluded.note;

insert into article_relations (source_article_id, target_article_id, relation_type, sort_order)
select source.id, target.id, seed.relation_type, seed.sort_order
from (
    values
        ('demo-article-creator-hub', 'demo-article-homepage-motion-budget', 'NEXT_READ', 10),
        ('demo-article-creator-hub', 'demo-article-category-strategy', 'RELATED', 20),
        ('demo-article-postgres-content-library', 'demo-article-testcontainers-migration', 'FOLLOW_UP', 10)
) as seed(source_slug, target_slug, relation_type, sort_order)
join articles source on source.slug = seed.source_slug
join articles target on target.slug = seed.target_slug
on conflict (source_article_id, target_article_id, relation_type) do update
set sort_order = excluded.sort_order;

insert into project_milestones (project_id, title, description, milestone_date, sort_order)
select project.id, seed.title, seed.description, current_date - seed.days_ago, seed.sort_order
from (
    values
        ('demo-project-content-console', '内容模型定稿', '确认文章、作品、灵感和主题配置边界。', 30, 10),
        ('demo-project-content-console', '后台工作台原型', '把 Dashboard、队列和规则页整理成 CMS 工作流。', 12, 20),
        ('demo-project-immersive-homepage', '沉浸式首页完成', 'WebGL、GSAP 和主题化内容模块完成第一版。', 8, 10)
) as seed(project_slug, title, description, days_ago, sort_order)
join portfolio_projects project on project.slug = seed.project_slug;

insert into project_links (project_id, link_type, label, url, safe_status, sort_order)
select project.id, seed.link_type, seed.label, seed.url, 'SAFE', seed.sort_order
from (
    values
        ('demo-project-content-console', 'DOC', '产品需求文档', 'https://example.com/creatorspace/spec', 10),
        ('demo-project-content-console', 'GITHUB', '源代码', 'https://github.com/SakuraCianna/CreatorSpace', 20),
        ('demo-project-immersive-homepage', 'DEMO', '前台演示', 'https://example.com/creatorspace', 10)
) as seed(project_slug, link_type, label, url, sort_order)
join portfolio_projects project on project.slug = seed.project_slug;

insert into project_process_notes (project_id, phase, title, body, sort_order)
select project.id, seed.phase, seed.title, seed.body, seed.sort_order
from (
    values
        ('demo-project-content-console', 'Research', '确认内容边界', '先把文章、作品、灵感和互动拆成稳定模块。', 10),
        ('demo-project-content-console', 'Design', '后台密度控制', '管理页优先服务扫描、比较和重复操作。', 20),
        ('demo-project-immersive-homepage', 'Motion', '一屏一个动效记忆点', '首页每个 section 只保留一种主要动效语言。', 10)
) as seed(project_slug, phase, title, body, sort_order)
join portfolio_projects project on project.slug = seed.project_slug;

insert into inspiration_sources (inspiration_id, source_type, title, url, author, captured_at)
select inspiration.id, seed.source_type, seed.title, seed.url, seed.author, now() - interval '3 days'
from (
    values
        ('玻璃质感不是透明度', 'URL', 'Awwwards Portfolio', 'https://www.awwwards.com/websites/portfolio/', 'Awwwards'),
        ('Prompt：把想法拆成任务卡', 'PROMPT', '内部 Prompt 模板', null, 'demo_creator'),
        ('后台列表的三层信息密度', 'QUOTE', '后台设计笔记', null, 'demo_designer')
) as seed(inspiration_title, source_type, title, url, author)
join inspiration_cards inspiration on inspiration.title = seed.inspiration_title;

insert into inspiration_relations (inspiration_id, target_type, target_id, relation_type)
select inspiration.id, seed.target_type, target.id, seed.relation_type
from (
    select 'Prompt：把想法拆成任务卡' as inspiration_title, 'ARTICLE' as target_type, id as target_id, 'SEED' as relation_type
    from articles where slug = 'demo-article-ai-writing-flow'
    union all
    select '后台列表的三层信息密度', 'PROJECT', id, 'REFERENCE'
    from portfolio_projects where slug = 'demo-project-content-console'
) seed
join inspiration_cards inspiration on inspiration.title = seed.inspiration_title
join lateral (select seed.target_id as id) target on true;

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
values
    ('HOME', null, 'home.hero', 'HERO', 'CreatorSpace 创作主页', '一个有主题风格的个人博客与作品展示平台。', '{"links": ["articles", "projects", "inspirations"]}'::jsonb, 10, true),
    ('HOME', null, 'home.featuredArticles', 'GALLERY', '精选文章', '按主题而不是时间线组织文章。', '{"source": "articles", "limit": 6}'::jsonb, 20, true),
    ('ABOUT', null, 'about.bio', 'TEXT', '关于创作者', '独立开发者，关注内容系统、AI 工作流和创作者工具。', '{"tone": "personal"}'::jsonb, 10, true);

insert into content_block_assets (block_id, file_id, usage_type, sort_order)
select block.id, file.id, 'CONTENT', 10
from content_blocks block
join file_resources file on file.file_name = 'demo-article-cover-creator-hub.webp'
where block.block_key = 'home.hero'
on conflict (block_id, file_id, usage_type) do nothing;

insert into comment_reactions (comment_id, user_id, reaction_type)
select comment.id, demo_user.id, 'LIKE'
from comments comment
join users demo_user on demo_user.username in ('demo_reader_02', 'demo_reader_03')
where comment.content = '展示评论：这篇创作中枢的拆解很适合做产品介绍页。'
on conflict (comment_id, user_id, reaction_type) do nothing;

insert into guestbook_entries (user_id, display_name, content, status, like_count, ip_address, user_agent)
select demo_user.id,
       demo_user.nickname,
       seed.content,
       seed.status,
       seed.like_count,
       seed.ip_address::inet,
       'CreatorSpace Demo Browser'
from (
    values
        ('demo_reader_01', '这个站点像一间可以慢慢逛的工作室。', 'APPROVED', 12, '127.0.2.10'),
        ('demo_reader_02', '希望之后看到完整的主题配置后台。', 'PENDING', 2, '127.0.2.11')
) as seed(username, content, status, like_count, ip_address)
join users demo_user on demo_user.username = seed.username;

insert into content_statistics (target_type, target_id, view_count, like_count, favorite_count, comment_count, last_viewed_at)
select 'ARTICLE', id, view_count, like_count, 18, comment_count, now() - interval '2 hours'
from articles
where slug in ('demo-article-creator-hub', 'demo-article-ai-writing-flow')
on conflict (target_type, target_id) do update
set view_count = excluded.view_count,
    like_count = excluded.like_count,
    favorite_count = excluded.favorite_count,
    comment_count = excluded.comment_count,
    last_viewed_at = excluded.last_viewed_at,
    updated_at = now();

insert into content_statistics (target_type, target_id, view_count, like_count, favorite_count, comment_count, last_viewed_at)
select 'PROJECT', id, 860, 52, 20, 3, now() - interval '4 hours'
from portfolio_projects
where slug in ('demo-project-content-console', 'demo-project-immersive-homepage')
on conflict (target_type, target_id) do update
set view_count = excluded.view_count,
    like_count = excluded.like_count,
    favorite_count = excluded.favorite_count,
    comment_count = excluded.comment_count,
    last_viewed_at = excluded.last_viewed_at,
    updated_at = now();

insert into search_logs (keyword, result_count, user_id, ip_address, user_agent, created_at)
select seed.keyword,
       seed.result_count,
       demo_user.id,
       seed.ip_address::inet,
       'CreatorSpace Demo Browser',
       now() - (seed.hours_ago || ' hours')::interval
from (
    values
        ('内容系统', 9, 'demo_reader_01', '127.0.3.10', 6),
        ('动效', 5, 'demo_reader_02', '127.0.3.11', 12),
        ('Prompt', 4, 'demo_reader_03', '127.0.3.12', 18)
) as seed(keyword, result_count, username, ip_address, hours_ago)
join users demo_user on demo_user.username = seed.username;

insert into daily_metrics (metric_date, metric_key, metric_value, detail_json)
select current_date - day_offset,
       metric_key,
       metric_value,
       jsonb_build_object('seed', true)
from (
    values
        (0, 'pv', 128),
        (1, 'pv', 112),
        (2, 'pv', 96),
        (0, 'article_published', 3),
        (0, 'comment_pending', 6)
) as seed(day_offset, metric_key, metric_value)
on conflict (metric_date, metric_key) do update
set metric_value = excluded.metric_value,
    detail_json = excluded.detail_json,
    updated_at = now();

insert into admin_audit_snapshots (target_type, target_id, snapshot_json, created_by)
select 'THEME',
       theme.id,
       jsonb_build_object('themeName', theme.theme_name, 'primaryColor', theme.primary_color, 'snapshot', 'before-demo-update'),
       demo_user.id
from theme_configs theme
join users demo_user on demo_user.username = 'demo_designer'
where theme.theme_name = 'glass-space';

insert into ai_agent_tasks (task_type, target_type, target_id, prompt, status, provider, model_name, created_by)
select 'ARTICLE_SUMMARY',
       'ARTICLE',
       article.id,
       '请为这篇文章生成更适合前台卡片展示的摘要。',
       'SUCCEEDED',
       'zhipu',
       'glm-4.6v-flash',
       demo_user.id
from articles article
join users demo_user on demo_user.username = 'demo_creator'
where article.slug = 'demo-article-creator-hub';

insert into ai_agent_messages (task_id, role, content, token_count)
select task.id, 'USER', task.prompt, 36
from ai_agent_tasks task
where task.task_type = 'ARTICLE_SUMMARY'
  and task.target_type = 'ARTICLE';

insert into ai_agent_messages (task_id, role, content, token_count)
select task.id, 'ASSISTANT', '这篇文章说明如何把个人站点整理成可长期维护的主题空间。', 48
from ai_agent_tasks task
where task.task_type = 'ARTICLE_SUMMARY'
  and task.target_type = 'ARTICLE';

insert into ai_suggestions (task_id, target_type, target_id, suggestion_type, content, status, adopted_by, adopted_at)
select task.id,
       task.target_type,
       task.target_id,
       'SUMMARY',
       '把个人站点整理成主题空间，让文章、作品和灵感都能被长期维护。',
       'ADOPTED',
       task.created_by,
       now() - interval '1 hour'
from ai_agent_tasks task
where task.task_type = 'ARTICLE_SUMMARY'
  and task.target_type = 'ARTICLE';
