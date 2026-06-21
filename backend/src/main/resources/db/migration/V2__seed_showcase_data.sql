-- CreatorSpace 展示数据。
-- 这份种子数据围绕“普通用户创作、管理员审核和前台互动”设计。

insert into users (username, nickname, password_hash, avatar_url, bio, status, created_at, updated_at, last_login_at)
values
    ('creator_lyra', '林星野', '$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u', '/uploads/demo/avatar-lyra.webp', '独立开发者，喜欢把博客、作品和灵感整理成一间长期开放的工作室。', 'ACTIVE', now() - interval '80 days', now() - interval '1 day', now() - interval '2 hours'),
    ('creator_nanzhi', '许南栀', '$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u', '/uploads/demo/avatar-nanzhi.webp', '产品设计师，关注界面叙事、视觉系统和创作者工具。', 'ACTIVE', now() - interval '72 days', now() - interval '2 days', now() - interval '3 hours'),
    ('creator_yanbai', '周砚白', '$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u', '/uploads/demo/avatar-yanbai.webp', '后端工程师，偏爱清晰的领域模型、数据库迁移和可验证的权限边界。', 'ACTIVE', now() - interval '68 days', now() - interval '4 hours', now() - interval '1 hour'),
    ('reader_haiyan', '海盐拿铁', '$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u', '/uploads/demo/avatar-reader.webp', '经常收藏创作者的文章和作品，也会认真写反馈。', 'ACTIVE', now() - interval '44 days', now() - interval '6 hours', now() - interval '30 minutes'),
    ('reader_morning', '晨间航线', '$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u', '/uploads/demo/avatar-reader-2.webp', '喜欢阅读技术复盘和设计过程。', 'ACTIVE', now() - interval '32 days', now() - interval '8 hours', now() - interval '4 hours')
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
where u.username in ('creator_lyra', 'creator_nanzhi', 'creator_yanbai', 'reader_haiyan', 'reader_morning')
on conflict (user_id, role_id) do nothing;

insert into user_profiles (user_id, display_name, headline, location, website_url, profile_json)
select u.id,
       u.nickname,
       seed.headline,
       seed.location,
       seed.website_url,
       seed.profile_json::jsonb
from (
    values
        ('creator_lyra', '把个人博客做成可被逛的创作空间', 'Shanghai', 'https://example.com/lyra', '{"skills": ["Vue", "Spring Boot", "AI workflow"], "creator": true}'),
        ('creator_nanzhi', '产品设计、视觉叙事和交互动效', 'Hangzhou', 'https://example.com/nanzhi', '{"skills": ["Design System", "Motion"], "creator": true}'),
        ('creator_yanbai', '数据库、权限和工程边界', 'Suzhou', 'https://example.com/yanbai', '{"skills": ["PostgreSQL", "Security"], "creator": true}'),
        ('reader_haiyan', '收藏值得反复阅读的创作笔记', 'Shanghai', null, '{"reader": true}'),
        ('reader_morning', '关注创意工具和个人网站', 'Nanjing', null, '{"reader": true}')
) as seed(username, headline, location, website_url, profile_json)
join users u on u.username = seed.username
on conflict (user_id) do update
set display_name = excluded.display_name,
    headline = excluded.headline,
    location = excluded.location,
    website_url = excluded.website_url,
    profile_json = excluded.profile_json,
    updated_at = now();

insert into user_friendships (requester_id, addressee_id, status, requested_at, accepted_at, updated_at)
select requester.id,
       addressee.id,
       seed.status,
       now() - (seed.days_ago || ' days')::interval,
       case when seed.status = 'ACCEPTED' then now() - ((seed.days_ago - 1) || ' days')::interval else null end,
       now() - ((seed.days_ago - 1) || ' days')::interval
from (
    values
        ('creator_lyra', 'creator_nanzhi', 'ACCEPTED', 24),
        ('creator_lyra', 'creator_yanbai', 'ACCEPTED', 22),
        ('reader_haiyan', 'creator_lyra', 'PENDING', 3)
) as seed(requester_username, addressee_username, status, days_ago)
join users requester on requester.username = seed.requester_username
join users addressee on addressee.username = seed.addressee_username
on conflict (requester_id, addressee_id) do update
set status = excluded.status,
    accepted_at = excluded.accepted_at,
    updated_at = excluded.updated_at;

insert into categories (module, name, slug, description, sort_order, enabled)
values
    ('ARTICLE', '创作札记', 'creator-notes', '普通用户发布的博客、教程和创作复盘。', 10, true),
    ('ARTICLE', '工程复盘', 'engineering-review', '技术实现、迁移、权限和质量门禁。', 20, true),
    ('PROJECT', 'Web 应用', 'web-app', '完整网站、应用和交互原型。', 10, true),
    ('PROJECT', '视觉实验', 'visual-lab', '动效、3D、视觉系统和创意展示。', 20, true),
    ('INSPIRATION', '灵感卡片', 'idea-card', '摘句、Prompt、参考链接和视觉碎片。', 10, true)
on conflict (module, slug) do update
set name = excluded.name,
    description = excluded.description,
    sort_order = excluded.sort_order,
    enabled = excluded.enabled,
    updated_at = now();

insert into tags (name, slug, color, weight)
values
    ('个人博客', 'personal-blog', '#315bff', 95),
    ('创意作品', 'creative-work', '#0f766e', 92),
    ('Spring Boot', 'spring-boot', '#2563eb', 88),
    ('Vue 3', 'vue-three', '#16a34a', 86),
    ('Three.js', 'three-js', '#7c3aed', 84),
    ('GSAP', 'gsap', '#f97316', 80),
    ('审核流', 'review-flow', '#b91c1c', 78),
    ('灵感墙', 'inspiration-wall', '#06b6d4', 76)
on conflict (slug) do update
set name = excluded.name,
    color = excluded.color,
    weight = excluded.weight,
    updated_at = now();

insert into file_resources (file_name, original_name, relative_path, public_url, file_type, file_size, storage_type, module, created_by)
select seed.file_name,
       seed.original_name,
       seed.relative_path,
       seed.public_url,
       seed.file_type,
       seed.file_size,
       'LOCAL',
       seed.module,
       u.id
from (
    values
        ('creator-article-cover.webp', 'article-cover.webp', 'article/2026/06/creator-article-cover.webp', '/uploads/article/2026/06/creator-article-cover.webp', 'image/webp', 184320, 'ARTICLE', 'creator_lyra'),
        ('creator-project-cover.webp', 'project-cover.webp', 'project/2026/06/creator-project-cover.webp', '/uploads/project/2026/06/creator-project-cover.webp', 'image/webp', 264320, 'PROJECT', 'creator_nanzhi'),
        ('theme-gallery-night.webp', 'theme-gallery-night.webp', 'cover/2026/06/theme-gallery-night.webp', '/uploads/cover/2026/06/theme-gallery-night.webp', 'image/webp', 324320, 'COVER', 'creator_nanzhi'),
        ('idea-card-source.md', 'idea-card-source.md', 'inspiration/2026/06/idea-card-source.md', '/uploads/inspiration/2026/06/idea-card-source.md', 'text/markdown', 2048, 'INSPIRATION', 'creator_lyra')
) as seed(file_name, original_name, relative_path, public_url, file_type, file_size, module, username)
join users u on u.username = seed.username;

insert into articles (
    title, slug, summary, content_markdown, cover_url, category_id, status, privacy_type,
    view_count, like_count, comment_count, is_top, is_recommend, submitted_at, reviewed_by, reviewed_at,
    review_note, publish_time, created_by, updated_by, created_at, updated_at
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
       case when seed.status in ('PENDING_REVIEW', 'PUBLISHED', 'PRIVATE', 'REJECTED') then now() - (seed.days_ago || ' days')::interval else null end,
       case when seed.status in ('PUBLISHED', 'PRIVATE', 'REJECTED') then admin_user.id else null end,
       case when seed.status in ('PUBLISHED', 'PRIVATE', 'REJECTED') then now() - ((seed.days_ago - 1) || ' days')::interval else null end,
       seed.review_note,
       case when seed.status in ('PUBLISHED', 'PRIVATE') then now() - ((seed.days_ago - 1) || ' days')::interval else null end,
       author.id,
       author.id,
       now() - ((seed.days_ago + 2) || ' days')::interval,
       now() - (seed.days_ago || ' days')::interval
from (
    values
        ('普通用户如何打造自己的主题博客', 'creator-theme-blog', '从头像、主题、文章和作品四个层面搭建个性化博客。', '## 主题博客\n普通用户先维护资料，再写文章、传作品，最后等待审核公开。', '/uploads/article/2026/06/creator-article-cover.webp', 'creator-notes', 'PUBLISHED', 'PUBLIC', 1260, 86, 2, true, true, 8, 'creator_lyra', '通过，适合作为新用户样例。'),
        ('待审核：我的 WebGL 首页实验', 'pending-webgl-homepage', '这篇文章展示待审核文章不会出现在公开列表。', '## 待审核\n这里记录 Three.js 和 GSAP 组合出的主视觉。', null, 'creator-notes', 'PENDING_REVIEW', 'PUBLIC', 0, 0, 0, false, false, 2, 'creator_nanzhi', null),
        ('被驳回：缺少来源的转载笔记', 'rejected-copy-note', '这篇文章用于展示审核驳回。', '## 待补充\n缺少来源链接，需要作者修改后重新提交。', null, 'creator-notes', 'REJECTED', 'PUBLIC', 0, 0, 0, false, false, 5, 'creator_lyra', '请补充原文来源和个人改写说明。'),
        ('草稿：创作中心的第一版规划', 'draft-creator-center', '草稿只属于作者本人。', '## 草稿\n先把创作入口拆成文章、作品、文件和互动。', null, 'engineering-review', 'DRAFT', 'PUBLIC', 0, 0, 0, false, false, 1, 'creator_yanbai', null),
        ('朋友可见的季度复盘', 'friends-quarter-review', '私密内容通过审核后仍不会进入公开列表。', '## 私密复盘\n这是一篇好友范围的创作复盘。', null, 'creator-notes', 'PRIVATE', 'FRIENDS', 42, 4, 0, false, false, 12, 'creator_lyra', '通过，按作者可见性保留为私密。')
) as seed(title, slug, summary, content_markdown, cover_url, category_slug, status, privacy_type, view_count, like_count, comment_count, is_top, is_recommend, days_ago, author_username, review_note)
join categories category on category.slug = seed.category_slug
join users author on author.username = seed.author_username
left join users admin_user on admin_user.username = '${adminUsername}'
on conflict (slug) do update
set title = excluded.title,
    summary = excluded.summary,
    content_markdown = excluded.content_markdown,
    cover_url = excluded.cover_url,
    status = excluded.status,
    privacy_type = excluded.privacy_type,
    submitted_at = excluded.submitted_at,
    reviewed_by = excluded.reviewed_by,
    reviewed_at = excluded.reviewed_at,
    review_note = excluded.review_note,
    publish_time = excluded.publish_time,
    updated_by = excluded.updated_by,
    updated_at = now();

insert into article_visibility_users (article_id, user_id, rule_type)
select article.id, visible_user.id, 'ALLOW'
from articles article
join users visible_user on visible_user.username in ('creator_nanzhi', 'creator_yanbai')
where article.slug = 'friends-quarter-review'
on conflict (article_id, user_id) do update
set rule_type = excluded.rule_type;

insert into article_versions (article_id, version_no, title, summary, content_markdown, created_by)
select article.id, 1, article.title || ' - 初稿', article.summary, article.content_markdown, article.created_by
from articles article
where article.slug in ('creator-theme-blog', 'pending-webgl-homepage')
on conflict (article_id, version_no) do update
set title = excluded.title,
    summary = excluded.summary,
    content_markdown = excluded.content_markdown;

insert into article_tags (article_id, tag_id)
select article.id, tag.id
from (
    values
        ('creator-theme-blog', 'personal-blog'),
        ('creator-theme-blog', 'review-flow'),
        ('pending-webgl-homepage', 'three-js'),
        ('pending-webgl-homepage', 'gsap'),
        ('draft-creator-center', 'spring-boot')
) as seed(article_slug, tag_slug)
join articles article on article.slug = seed.article_slug
join tags tag on tag.slug = seed.tag_slug
on conflict (article_id, tag_id) do nothing;

insert into portfolio_projects (
    title, slug, description, cover_url, project_type, tech_stack, github_url, demo_url, video_url,
    content_markdown, status, is_recommend, sort_order, started_at, ended_at, submitted_at, reviewed_by,
    reviewed_at, review_note, created_by, updated_by, created_at, updated_at
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
       null,
       case when seed.status in ('PENDING_REVIEW', 'VISIBLE', 'REJECTED') then now() - (seed.days_ago || ' days')::interval else null end,
       case when seed.status in ('VISIBLE', 'REJECTED') then admin_user.id else null end,
       case when seed.status in ('VISIBLE', 'REJECTED') then now() - ((seed.days_ago - 1) || ' days')::interval else null end,
       seed.review_note,
       author.id,
       author.id,
       now() - ((seed.days_ago + 3) || ' days')::interval,
       now() - (seed.days_ago || ' days')::interval
from (
    values
        ('CreatorSpace 创作中心', 'creator-center-workbench', '普通用户写博客、传作品、管理文件和查看审核状态的工作台。', '/uploads/project/2026/06/creator-project-cover.webp', 'WEB_APP', '["Vue 3", "Spring Boot 3.5", "PostgreSQL"]', 'https://github.com/SakuraCianna/CreatorSpace', 'https://example.com/creator-center', null, '## 创作中心\n让普通用户成为内容生产者，管理员只负责审核和运营。', 'VISIBLE', true, 10, 40, 7, 'creator_lyra', '审核通过，进入作品展厅。'),
        ('待审核：WebGL 视觉海报', 'pending-webgl-poster', '一组使用 Three.js 与粒子动效完成的视觉实验。', null, 'VISUAL_LAB', '["Three.js", "GSAP"]', null, 'https://example.com/webgl-poster', null, '## 待审核作品\n需要确认素材授权。', 'PENDING_REVIEW', false, 20, 12, 2, 'creator_nanzhi', null),
        ('被驳回：未标注授权的素材包', 'rejected-asset-pack', '用于展示作品审核驳回状态。', null, 'ASSET_PACK', '["Design"]', null, null, null, '## 待修改\n素材来源未标注。', 'REJECTED', false, 30, 16, 5, 'creator_nanzhi', '请补充素材授权来源。'),
        ('草稿：评论审核可视化', 'draft-review-dashboard', '作者尚未提交的作品草稿。', null, 'DASHBOARD', '["Spring Boot", "Vue 3"]', null, null, null, '## 草稿\n先整理审核队列的指标。', 'DRAFT', false, 40, 4, 1, 'creator_yanbai', null)
) as seed(title, slug, description, cover_url, project_type, tech_stack, github_url, demo_url, video_url, content_markdown, status, is_recommend, sort_order, started_days_ago, days_ago, author_username, review_note)
join users author on author.username = seed.author_username
left join users admin_user on admin_user.username = '${adminUsername}'
on conflict (slug) do update
set title = excluded.title,
    description = excluded.description,
    cover_url = excluded.cover_url,
    project_type = excluded.project_type,
    tech_stack = excluded.tech_stack,
    status = excluded.status,
    is_recommend = excluded.is_recommend,
    submitted_at = excluded.submitted_at,
    reviewed_by = excluded.reviewed_by,
    reviewed_at = excluded.reviewed_at,
    review_note = excluded.review_note,
    updated_by = excluded.updated_by,
    updated_at = now();

insert into project_tags (project_id, tag_id)
select project.id, tag.id
from (
    values
        ('creator-center-workbench', 'creative-work'),
        ('creator-center-workbench', 'vue-three'),
        ('creator-center-workbench', 'spring-boot'),
        ('pending-webgl-poster', 'three-js'),
        ('pending-webgl-poster', 'gsap')
) as seed(project_slug, tag_slug)
join portfolio_projects project on project.slug = seed.project_slug
join tags tag on tag.slug = seed.tag_slug
on conflict (project_id, tag_id) do nothing;

insert into project_images (project_id, image_url, caption, sort_order)
select project.id, '/uploads/project/2026/06/creator-project-cover.webp', '创作中心概览', 10
from portfolio_projects project
where project.slug = 'creator-center-workbench';

insert into project_milestones (project_id, title, description, milestone_date, sort_order)
select project.id, '角色边界重设', '确认普通用户是创作者，管理员是审核者。', current_date - 7, 10
from portfolio_projects project
where project.slug = 'creator-center-workbench';

insert into project_links (project_id, link_type, label, url, safe_status, sort_order)
select project.id, 'DEMO', '在线演示', 'https://example.com/creator-center', 'SAFE', 10
from portfolio_projects project
where project.slug = 'creator-center-workbench';

insert into project_process_notes (project_id, phase, title, body, sort_order)
select project.id, 'Review Flow', '从发布改成审核', '作品先由普通用户提交，再由管理员决定展示、驳回或下架。', 10
from portfolio_projects project
where project.slug = 'creator-center-workbench';

insert into inspiration_cards (title, content, image_url, card_type, source_url, color, is_public, sort_order, created_by)
select seed.title, seed.content, seed.image_url, seed.card_type, seed.source_url, seed.color, seed.is_public, seed.sort_order, u.id
from (
    values
        ('创作中心信息架构', '文章、作品、文件、互动四个入口放在普通用户侧。', null, 'TEXT', null, '#315bff', true, 10, 'creator_lyra'),
        ('Prompt：审核意见生成', '请把审核意见写得具体、可执行，并避免否定创作者本人。', null, 'PROMPT', null, '#7c3aed', true, 20, 'creator_yanbai'),
        ('视觉参考：展厅式作品卡', null, '/uploads/project/2026/06/creator-project-cover.webp', 'IMAGE', 'https://example.com/gallery-reference', '#0f766e', true, 30, 'creator_nanzhi')
) as seed(title, content, image_url, card_type, source_url, color, is_public, sort_order, username)
join users u on u.username = seed.username;

insert into inspiration_tags (inspiration_id, tag_id)
select inspiration.id, tag.id
from inspiration_cards inspiration
join tags tag on tag.slug in ('inspiration-wall', 'creative-work')
where inspiration.title in ('创作中心信息架构', '视觉参考：展厅式作品卡')
on conflict (inspiration_id, tag_id) do nothing;

insert into inspiration_sources (inspiration_id, source_type, title, url, author, captured_at)
select inspiration.id, 'URL', 'CreatorSpace 需求讨论', 'https://example.com/creator-role-boundary', 'CreatorSpace Team', now() - interval '1 day'
from inspiration_cards inspiration
where inspiration.title = '创作中心信息架构';

insert into inspiration_relations (inspiration_id, target_type, target_id, relation_type)
select inspiration.id, 'ARTICLE', article.id, 'SEED'
from inspiration_cards inspiration
join articles article on article.slug = 'creator-theme-blog'
where inspiration.title = '创作中心信息架构';

insert into comments (target_type, target_id, user_id, content, status, depth, like_count, ip_address, user_agent)
select 'ARTICLE', article.id, reader.id, '普通用户能写博客以后，这个平台才真的像创作者社区。', 'APPROVED', 0, 2, '127.0.2.10'::inet, 'CreatorSpace Demo Browser'
from articles article
join users reader on reader.username = 'reader_haiyan'
where article.slug = 'creator-theme-blog';

insert into comments (target_type, target_id, user_id, content, status, depth, like_count, ip_address, user_agent)
select 'PROJECT', project.id, reader.id, '希望作品页后面能支持多图时间线。', 'PENDING', 0, 0, '127.0.2.11'::inet, 'CreatorSpace Demo Browser'
from portfolio_projects project
join users reader on reader.username = 'reader_morning'
where project.slug = 'creator-center-workbench';

insert into sensitive_words (word, match_type, severity, created_by)
select '恶意广告', 'CONTAINS', 'REJECT', admin_user.id
from users admin_user
where admin_user.username = '${adminUsername}'
on conflict (word) do update
set match_type = excluded.match_type,
    severity = excluded.severity,
    enabled = true,
    updated_at = now();

update theme_configs
set display_name = 'Creator Gallery',
    primary_color = '#315bff',
    background_type = 'webgl',
    background_image = '/uploads/cover/2026/06/theme-gallery-night.webp',
    font_family = 'Inter, system-ui, sans-serif',
    card_style = 'glass',
    layout_type = 'creator-gallery',
    is_active = true,
    config_json = '{"density": "immersive", "motion": "rich", "creatorMode": true}'::jsonb,
    updated_at = now()
where theme_name = 'glass-space';

insert into theme_configs (theme_name, display_name, primary_color, background_type, background_image, font_family, card_style, layout_type, is_active, config_json)
values ('editorial-focus', 'Editorial Focus', '#0f766e', 'gradient', null, 'Inter, system-ui, sans-serif', 'outline', 'editorial', false, '{"density": "reading", "motion": "calm"}'::jsonb)
on conflict (theme_name) do update
set display_name = excluded.display_name,
    primary_color = excluded.primary_color,
    background_type = excluded.background_type,
    config_json = excluded.config_json,
    updated_at = now();

insert into site_profiles (profile_key, display_name, headline, avatar_url, bio, contact_email, location, profile_json, is_active)
values (
    'creator-community',
    'CreatorSpace',
    '普通用户写博客、上传作品，管理员负责审核与策展',
    '/uploads/demo/avatar-lyra.webp',
    '一个面向创作者的个人主题博客与创意作品展示平台。',
    'demo@example.com',
    'Shanghai',
    '{"positioning": "creator-first", "sections": ["articles", "projects", "inspirations"]}'::jsonb,
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
    ('文章', '/articles', 'book-open', 'primary', 10, true, '{}'::jsonb),
    ('作品', '/projects', 'images', 'primary', 20, true, '{}'::jsonb),
    ('灵感', '/inspirations', 'lightbulb', 'primary', 30, true, '{}'::jsonb),
    ('创作', '/creator', 'pen-line', 'primary', 40, true, '{"requiresAuth": true}'::jsonb),
    ('搜索', '/search', 'search', 'primary', 50, true, '{}'::jsonb),
    ('关于', '/about', 'info', 'primary', 60, true, '{}'::jsonb);

insert into social_links (platform, label, url, icon, sort_order, visible)
values
    ('github', 'GitHub', 'https://github.com/SakuraCianna/CreatorSpace', 'github', 10, true),
    ('mail', 'Mail', 'mailto:demo@example.com', 'mail', 20, true);

insert into page_configs (page_key, title, slug, seo_title, seo_description, content_json, layout_json, status)
values
    ('home', 'CreatorSpace 首页', 'home', 'CreatorSpace - 创作者主题空间', '普通用户可以写博客、上传作品并参与互动。', '{"modules": ["hero", "creatorArticles", "creatorProjects", "inspirations"]}'::jsonb, '{"density": "immersive"}'::jsonb, 'PUBLISHED'),
    ('about', '关于 CreatorSpace', 'about', '关于 CreatorSpace', '了解创作者社区、审核机制和主题体验。', '{"sections": ["mission", "roles", "contact"]}'::jsonb, '{"density": "editorial"}'::jsonb, 'PUBLISHED')
on conflict (page_key) do update
set title = excluded.title,
    seo_title = excluded.seo_title,
    seo_description = excluded.seo_description,
    content_json = excluded.content_json,
    layout_json = excluded.layout_json,
    status = excluded.status,
    updated_at = now();

insert into site_configs (config_key, config_value, description)
values
    ('site.identity', '{"name": "CreatorSpace", "slogan": "创作者先行的主题博客与作品展厅"}'::jsonb, '站点身份'),
    ('site.creatorPolicy', '{"articleReview": true, "projectReview": true, "adminRole": "reviewer"}'::jsonb, '普通用户创作和管理员审核策略')
on conflict (config_key) do update
set config_value = excluded.config_value,
    description = excluded.description,
    updated_at = now();

insert into theme_variants (theme_id, variant_name, density, color_tokens, font_tokens, motion_tokens, card_tokens)
select theme.id, 'Creator Default', 'comfortable', '{"primary": "#315bff", "accent": "#0f766e"}'::jsonb, '{"display": "Inter"}'::jsonb, '{"level": "rich"}'::jsonb, '{"radius": 8}'::jsonb
from theme_configs theme
where theme.theme_name = 'glass-space'
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
join file_resources file on file.file_name = 'theme-gallery-night.webp'
where theme.theme_name = 'glass-space';

insert into theme_versions (theme_id, version_no, snapshot_json, created_by)
select theme.id, 1, jsonb_build_object('themeName', theme.theme_name, 'creatorMode', true), admin_user.id
from theme_configs theme
join users admin_user on admin_user.username = '${adminUsername}'
where theme.theme_name = 'glass-space'
on conflict (theme_id, version_no) do update
set snapshot_json = excluded.snapshot_json;

insert into tag_aliases (tag_id, alias)
select tag.id, seed.alias
from (
    values
        ('personal-blog', '主题博客'),
        ('creative-work', '作品集'),
        ('review-flow', '审核机制')
) as seed(slug, alias)
join tags tag on tag.slug = seed.slug
on conflict (tag_id, alias) do nothing;

insert into article_series (title, slug, description, cover_url, sort_order, status, created_by)
select '创作中心建设日志', 'creator-center-journal', '从角色边界到审核流的系列记录。', '/uploads/article/2026/06/creator-article-cover.webp', 10, 'ACTIVE', u.id
from users u
where u.username = 'creator_lyra'
on conflict (slug) do update
set title = excluded.title,
    description = excluded.description,
    cover_url = excluded.cover_url,
    sort_order = excluded.sort_order,
    updated_at = now();

insert into article_series_items (series_id, article_id, sort_order, note)
select series.id, article.id, 10, '系列入口'
from article_series series
join articles article on article.slug = 'creator-theme-blog'
where series.slug = 'creator-center-journal'
on conflict (series_id, article_id) do update
set sort_order = excluded.sort_order,
    note = excluded.note;

insert into article_relations (source_article_id, target_article_id, relation_type, sort_order)
select source.id, target.id, 'RELATED', 10
from articles source
join articles target on target.slug = 'pending-webgl-homepage'
where source.slug = 'creator-theme-blog'
on conflict (source_article_id, target_article_id, relation_type) do update
set sort_order = excluded.sort_order;

insert into file_resource_references (file_id, target_type, target_id, usage_type)
select file.id, 'ARTICLE', article.id, 'COVER'
from file_resources file
join articles article on article.slug = 'creator-theme-blog'
where file.file_name = 'creator-article-cover.webp'
on conflict (file_id, target_type, target_id, usage_type) do nothing;

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
values
    ('HOME', null, 'home.creatorHero', 'HERO', '创作者先行', '普通用户写博客、上传作品，管理员负责审核与策展。', '{"cta": "/creator"}'::jsonb, 10, true),
    ('ABOUT', null, 'about.roles', 'TEXT', '角色边界', '游客浏览，普通用户创作和互动，管理员审核与运营。', '{"tone": "product"}'::jsonb, 10, true);

insert into content_block_assets (block_id, file_id, usage_type, sort_order)
select block.id, file.id, 'CONTENT', 10
from content_blocks block
join file_resources file on file.file_name = 'theme-gallery-night.webp'
where block.block_key = 'home.creatorHero'
on conflict (block_id, file_id, usage_type) do nothing;

insert into like_records (target_type, target_id, user_id)
select 'ARTICLE', article.id, reader.id
from articles article
join users reader on reader.username in ('reader_haiyan', 'reader_morning')
where article.slug = 'creator-theme-blog'
on conflict (target_type, target_id, user_id) do nothing;

insert into favorite_records (target_type, target_id, user_id)
select 'PROJECT', project.id, reader.id
from portfolio_projects project
join users reader on reader.username in ('reader_haiyan', 'reader_morning')
where project.slug = 'creator-center-workbench'
on conflict (target_type, target_id, user_id) do nothing;

insert into comment_reactions (comment_id, user_id, reaction_type)
select comment.id, user_row.id, 'LIKE'
from comments comment
join users user_row on user_row.username = 'reader_morning'
where comment.content = '普通用户能写博客以后，这个平台才真的像创作者社区。'
on conflict (comment_id, user_id, reaction_type) do nothing;

insert into guestbook_entries (user_id, display_name, content, status, like_count, ip_address, user_agent)
select user_row.id, user_row.nickname, '期待看到更多普通用户上传的作品。', 'APPROVED', 6, '127.0.3.10'::inet, 'CreatorSpace Demo Browser'
from users user_row
where user_row.username = 'reader_haiyan';

insert into friend_links (site_name, site_url, logo_url, description, status, sort_order)
values ('Creator Lab', 'https://example.com/creator-lab', '/uploads/demo/avatar-nanzhi.webp', '创作者案例站点。', 'APPROVED', 10);

insert into visit_logs (path, target_type, target_id, ip_address, user_agent, referer, device_type, browser, operating_system, created_at)
select '/articles/creator-theme-blog', 'ARTICLE', article.id, '127.0.4.10'::inet, 'CreatorSpace Demo Browser', '/', 'desktop', 'Chrome', 'Windows', now() - interval '2 hours'
from articles article
where article.slug = 'creator-theme-blog';

insert into content_statistics (target_type, target_id, view_count, like_count, favorite_count, comment_count, last_viewed_at)
select 'ARTICLE', article.id, 1260, 86, 14, 2, now() - interval '2 hours'
from articles article
where article.slug = 'creator-theme-blog'
on conflict (target_type, target_id) do update
set view_count = excluded.view_count,
    like_count = excluded.like_count,
    favorite_count = excluded.favorite_count,
    comment_count = excluded.comment_count,
    last_viewed_at = excluded.last_viewed_at,
    updated_at = now();

insert into content_statistics (target_type, target_id, view_count, like_count, favorite_count, comment_count, last_viewed_at)
select 'PROJECT', project.id, 920, 48, 20, 1, now() - interval '3 hours'
from portfolio_projects project
where project.slug = 'creator-center-workbench'
on conflict (target_type, target_id) do update
set view_count = excluded.view_count,
    like_count = excluded.like_count,
    favorite_count = excluded.favorite_count,
    comment_count = excluded.comment_count,
    last_viewed_at = excluded.last_viewed_at,
    updated_at = now();

insert into search_logs (keyword, result_count, user_id, ip_address, user_agent, created_at)
select '创作中心', 3, user_row.id, '127.0.5.10'::inet, 'CreatorSpace Demo Browser', now() - interval '1 hour'
from users user_row
where user_row.username = 'reader_haiyan';

insert into daily_metrics (metric_date, metric_key, metric_value, detail_json)
values
    (current_date, 'creator.article.submitted', 2, '{"seed": true}'::jsonb),
    (current_date, 'creator.project.submitted', 2, '{"seed": true}'::jsonb),
    (current_date, 'admin.review.pending', 2, '{"seed": true}'::jsonb)
on conflict (metric_date, metric_key) do update
set metric_value = excluded.metric_value,
    detail_json = excluded.detail_json,
    updated_at = now();

insert into operation_logs (operator_id, operation, module, target_type, target_id, request_method, request_path, detail_json)
select admin_user.id, '审核通过文章', 'ARTICLE', 'ARTICLE', article.id, 'PUT', '/api/admin/articles/' || article.id || '/approve', '{"seed": true}'::jsonb
from users admin_user
join articles article on article.slug = 'creator-theme-blog'
where admin_user.username = '${adminUsername}';

insert into admin_audit_snapshots (target_type, target_id, snapshot_json, created_by)
select 'ARTICLE', article.id, jsonb_build_object('status', article.status, 'reviewNote', article.review_note), admin_user.id
from articles article
join users admin_user on admin_user.username = '${adminUsername}'
where article.slug = 'creator-theme-blog';

insert into ai_agent_tasks (task_type, target_type, target_id, prompt, status, provider, model_name, created_by)
select 'REVIEW_ASSIST', 'ARTICLE', article.id, '请帮管理员生成温和、可执行的文章审核意见。', 'SUCCEEDED', 'zhipu', 'glm-4.6v-flash', admin_user.id
from articles article
join users admin_user on admin_user.username = '${adminUsername}'
where article.slug = 'pending-webgl-homepage';

insert into ai_agent_messages (task_id, role, content, token_count)
select task.id, 'USER', task.prompt, 32
from ai_agent_tasks task
where task.task_type = 'REVIEW_ASSIST';

insert into ai_agent_messages (task_id, role, content, token_count)
select task.id, 'ASSISTANT', '建议补充素材授权说明和动效性能说明。', 42
from ai_agent_tasks task
where task.task_type = 'REVIEW_ASSIST';

insert into ai_suggestions (task_id, target_type, target_id, suggestion_type, content, status)
select task.id, task.target_type, task.target_id, 'REVIEW_NOTE', '请补充素材授权说明，并说明 WebGL 动效在移动端的性能处理。', 'PENDING'
from ai_agent_tasks task
where task.task_type = 'REVIEW_ASSIST';
