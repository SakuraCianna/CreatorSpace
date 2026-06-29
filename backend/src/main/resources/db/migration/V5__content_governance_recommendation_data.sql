alter table visit_logs
    add column if not exists user_id bigint references users(id) on delete set null;

comment on column visit_logs.user_id is '访问用户主键, 未登录访客为空';

create index if not exists idx_visit_logs_user_created_at on visit_logs(user_id, created_at desc);

insert into users (username, nickname, password_hash, avatar_url, bio, status, created_at, updated_at, last_login_at)
select seed.username,
       seed.nickname,
       '$2a$04$in9e7kWY5HdXRYYeRYd//O2p3R2AJiaeWy8/pgLrirdRdZtxDIG8u',
       seed.avatar_url,
       seed.bio,
       'ACTIVE',
       now() - seed.age,
       now() - interval '1 day',
       now() - seed.last_seen
from (
    values
        ('reader_frontend', '前端观察员', '/uploads/avatars/avatar-reader-frontend.webp', '偏爱前端体验、设计系统和交互细节。', interval '44 days', interval '2 hours'),
        ('reader_backend', '后端记录员', '/uploads/avatars/avatar-reader-backend.webp', '关注 Spring Boot、数据库结构和接口可靠性。', interval '39 days', interval '5 hours'),
        ('reader_visual', '视觉实验员', '/uploads/avatars/avatar-reader-visual.webp', '喜欢 Three.js、动效和视觉实验。', interval '31 days', interval '1 day'),
        ('reader_writer', '写作整理师', '/uploads/avatars/avatar-reader-writer.webp', '常读主题博客、阅读路径和内容归档。', interval '27 days', interval '3 days')
) as seed(username, nickname, avatar_url, bio, age, last_seen)
on conflict (username) do update
set nickname = excluded.nickname,
    avatar_url = excluded.avatar_url,
    bio = excluded.bio,
    updated_at = now();

insert into user_roles (user_id, role_id)
select u.id, r.id
from users u
join roles r on r.code = 'USER'
where u.username in ('reader_frontend', 'reader_backend', 'reader_visual', 'reader_writer')
on conflict (user_id, role_id) do nothing;

insert into user_profiles (user_id, display_name, headline, location, website_url, profile_json)
select u.id,
       u.nickname,
       case u.username
           when 'reader_frontend' then '前端审美与体验巡检'
           when 'reader_backend' then '后端接口与数据建模'
           when 'reader_visual' then '视觉实验与动效叙事'
           else '主题阅读与内容整理'
       end,
       '线上创作社区',
       'https://example.com/' || u.username,
       jsonb_build_object('source', 'content-ops', 'persona', u.username)
from users u
where u.username in ('reader_frontend', 'reader_backend', 'reader_visual', 'reader_writer')
on conflict (user_id) do update
set display_name = excluded.display_name,
    headline = excluded.headline,
    profile_json = excluded.profile_json,
    updated_at = now();

insert into categories (module, name, slug, description, sort_order, enabled)
values
    ('ARTICLE', '前端体验', 'frontend-experience', '前台界面、响应式布局、可读性和交互体验复盘。', 30, true),
    ('ARTICLE', '后端工程', 'backend-engineering', '接口、权限、数据建模和运行质量记录。', 40, true),
    ('ARTICLE', '主题博客', 'theme-blog', '围绕主题写作、阅读路径和长期归档的文章。', 50, true),
    ('PROJECT', '交互应用', 'interactive-apps', '可操作、可在线体验的前端应用作品。', 30, true),
    ('PROJECT', '内容系统', 'content-systems', 'CMS、后台工作台和内容运营工具。', 40, true),
    ('INSPIRATION', '界面灵感', 'interface-inspiration', '组件、布局、动效和视觉参考。', 30, true)
on conflict (module, slug) do update
set name = excluded.name,
    description = excluded.description,
    sort_order = excluded.sort_order,
    enabled = excluded.enabled,
    updated_at = now();

insert into tags (name, slug, color, weight)
values
    ('前端体验', 'frontend-experience', '#315bff', 98),
    ('响应式布局', 'responsive-layout', '#0f766e', 94),
    ('可读性', 'readability', '#7c3aed', 91),
    ('推荐算法', 'recommendation', '#f97316', 88),
    ('主题博客', 'theme-blog', '#db2777', 86),
    ('阅读路径', 'reading-path', '#0891b2', 84),
    ('Spring Security', 'spring-security', '#16a34a', 78),
    ('Flyway', 'flyway', '#b45309', 72),
    ('数据治理', 'data-governance', '#64748b', 70),
    ('后台工作台', 'admin-workbench', '#4f46e5', 82),
    ('灵感墙', 'inspiration-wall', '#9333ea', 80),
    ('图像展示', 'image-gallery', '#0284c7', 76)
on conflict (slug) do update
set name = excluded.name,
    color = excluded.color,
    weight = greatest(tags.weight, excluded.weight),
    updated_at = now();

insert into articles (
    title, slug, summary, content_markdown, cover_url, category_id, status, privacy_type,
    view_count, like_count, comment_count, is_top, is_recommend, submitted_at, reviewed_by, reviewed_at,
    publish_time, created_by, updated_by
)
select seed.title,
       seed.slug,
       seed.summary,
       seed.content_markdown,
       seed.cover_url,
       c.id,
       'PUBLISHED',
       'PUBLIC',
       seed.view_count,
       seed.like_count,
       seed.comment_count,
       seed.is_top,
       seed.is_recommend,
       now() - seed.review_age - interval '4 hours',
       reviewer.id,
       now() - seed.review_age,
       now() - seed.publish_age - interval '720 days',
       author.id,
       author.id
from (
    values
        ('主页标签推荐如何读懂用户兴趣', 'homepage-tag-recommendation', '用访问记录、标签关系和随机扰动，让首页标签云更像个性化入口。', '## 目标\n\n首页标签不应该只是后台权重排序。登录用户会留下文章访问记录，可以把最近访问的文章标签提权；游客则混合热度和随机性，形成每次访问略有变化的发现感。\n\n## 算法\n\n最近访问越新，分数越高；内容引用越多，基础分越稳定；随机扰动负责探索。', '/uploads/article/2026/06/homepage-tag-recommendation.webp', 'frontend-experience', 1860, 132, 18, false, true, interval '2 days', interval '2 days'),
        ('把表单控件做成统一设计语言', 'unified-form-controls', '下拉框、文件上传、输入框和胶囊按钮应当共享清晰的状态反馈。', '## 背景\n\n不同页面的控件如果各写各的，会出现 hover 文字消失、控件高度拉长、移动端溢出等问题。\n\n## 处理\n\n页面样式保持局部 scoped，公共 token 只负责颜色、圆角和阴影。', '/uploads/article/2026/06/unified-form-controls.webp', 'frontend-experience', 1420, 98, 12, false, true, interval '5 days', interval '5 days'),
        ('Spring Security 白名单与公开推荐接口', 'security-public-recommendation-api', '公开接口需要允许游客访问，同时在有 token 时读取登录上下文。', '## 要点\n\n推荐标签接口是公开接口，但请求里可能带 Bearer Token。过滤器会解析 token 并写入 SecurityContext，Controller 可以通过 AuthenticationPrincipal 读取当前用户。\n\n## 边界\n\n无 token 时按游客处理，不返回 401。', '/uploads/article/2026/06/security-public-recommendation.webp', 'backend-engineering', 980, 64, 8, false, true, interval '8 days', interval '8 days'),
        ('内容治理怎样支撑真实巡检', 'content-governance-field-notes', '把运营状态、内容关系和后台动作整理成可复盘的巡检材料。', '## 原则\n\n内容治理要覆盖用户、分类、标签、文章、作品、灵感、互动、访问、搜索、统计和后台日志。\n\n## 复盘\n\n统一的命名、稳定的资源归档和连续的操作记录，可以让团队更快判断页面密度、审核状态和内容关系是否健康。', '/uploads/article/2026/06/content-governance-field-notes.webp', 'backend-engineering', 1210, 77, 9, false, false, interval '11 days', interval '11 days'),
        ('主题博客的阅读路径设计', 'theme-blog-reading-path-design', '文章不只按时间排列，也应当按系列、标签和关联文章形成路径。', '## 阅读路径\n\n主题博客需要让读者知道从哪里进入、下一篇读什么、哪些灵感最终长成了文章。\n\n## 首页入口\n\n标签云是入口，不是装饰。它应该能反映站点最近的创作重心，也能适度响应用户兴趣。', '/uploads/article/2026/06/theme-blog-reading-path.webp', 'theme-blog', 1560, 119, 16, false, true, interval '14 days', interval '14 days'),
        ('灵感墙如何连接文章和作品', 'inspiration-wall-relations', '灵感卡片应当能回到文章、作品和参考来源，而不是孤立碎片。', '## 关联\n\n灵感和作品之间可以是来源、种子或衍生关系。详情抽屉让用户在不离开页面的情况下理解上下文。\n\n## 展示\n\n卡片密度要适中，移动端需要保持可读。', '/uploads/article/2026/06/inspiration-wall-relations.webp', 'theme-blog', 1080, 74, 7, false, false, interval '17 days', interval '17 days')
) as seed(title, slug, summary, content_markdown, cover_url, category_slug, view_count, like_count, comment_count, is_top, is_recommend, review_age, publish_age)
join categories c on c.module = 'ARTICLE' and c.slug = seed.category_slug
join users author on author.username = 'creator_lyra'
left join users reviewer on reviewer.username = '${adminUsername}'
on conflict (slug) do update
set summary = excluded.summary,
    content_markdown = excluded.content_markdown,
    category_id = excluded.category_id,
    status = excluded.status,
    privacy_type = excluded.privacy_type,
    view_count = greatest(articles.view_count, excluded.view_count),
    like_count = greatest(articles.like_count, excluded.like_count),
    comment_count = greatest(articles.comment_count, excluded.comment_count),
    is_top = excluded.is_top,
    is_recommend = excluded.is_recommend,
    publish_time = excluded.publish_time,
    updated_at = now();

insert into article_tags (article_id, tag_id)
select article.id, tag.id
from (
    values
        ('homepage-tag-recommendation', 'frontend-experience'),
        ('homepage-tag-recommendation', 'recommendation'),
        ('homepage-tag-recommendation', 'theme-blog'),
        ('unified-form-controls', 'frontend-experience'),
        ('unified-form-controls', 'responsive-layout'),
        ('unified-form-controls', 'readability'),
        ('security-public-recommendation-api', 'spring-security'),
        ('security-public-recommendation-api', 'recommendation'),
        ('content-governance-field-notes', 'flyway'),
        ('content-governance-field-notes', 'data-governance'),
        ('theme-blog-reading-path-design', 'theme-blog'),
        ('theme-blog-reading-path-design', 'reading-path'),
        ('inspiration-wall-relations', 'inspiration-wall'),
        ('inspiration-wall-relations', 'theme-blog')
) as seed(article_slug, tag_slug)
join articles article on article.slug = seed.article_slug
join tags tag on tag.slug = seed.tag_slug
on conflict (article_id, tag_id) do nothing;

insert into article_versions (article_id, version_no, title, summary, content_markdown, created_by)
select article.id, 1, article.title || ' - 公开稿', article.summary, article.content_markdown, article.created_by
from articles article
where article.slug in (
    'homepage-tag-recommendation',
    'unified-form-controls',
    'security-public-recommendation-api',
    'content-governance-field-notes',
    'theme-blog-reading-path-design',
    'inspiration-wall-relations'
)
on conflict (article_id, version_no) do nothing;

insert into article_series (title, slug, description, cover_url, sort_order, status, created_by)
select '首页推荐与内容路径实验',
       'homepage-recommendation-path',
       '围绕首页标签推荐、主题博客路径和灵感关联的一组文章。',
       '/uploads/article/2026/06/homepage-tag-recommendation.webp',
       20,
       'ACTIVE',
       u.id
from users u
where u.username = 'creator_lyra'
on conflict (slug) do update
set title = excluded.title,
    description = excluded.description,
    updated_at = now();

insert into article_series_items (series_id, article_id, sort_order, note)
select series.id, article.id, seed.sort_order, seed.note
from (
    values
        ('homepage-tag-recommendation', 10, '从推荐入口开始'),
        ('theme-blog-reading-path-design', 20, '解释主题博客路径'),
        ('inspiration-wall-relations', 30, '补齐灵感关联')
) as seed(article_slug, sort_order, note)
join article_series series on series.slug = 'homepage-recommendation-path'
join articles article on article.slug = seed.article_slug
on conflict (series_id, article_id) do update
set sort_order = excluded.sort_order,
    note = excluded.note;

insert into article_relations (source_article_id, target_article_id, relation_type, sort_order)
select source.id, target.id, seed.relation_type, seed.sort_order
from (
    values
        ('homepage-tag-recommendation', 'theme-blog-reading-path-design', 'NEXT_READ', 10),
        ('homepage-tag-recommendation', 'security-public-recommendation-api', 'RELATED', 20),
        ('theme-blog-reading-path-design', 'inspiration-wall-relations', 'RELATED', 30)
) as seed(source_slug, target_slug, relation_type, sort_order)
join articles source on source.slug = seed.source_slug
join articles target on target.slug = seed.target_slug
where source.id <> target.id
on conflict do nothing;

insert into portfolio_projects (
    title, slug, description, cover_url, project_type, tech_stack, github_url, demo_url, video_url,
    content_markdown, status, is_recommend, sort_order, started_at, ended_at, submitted_at, reviewed_by,
    reviewed_at, created_by, updated_by
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
       'VISIBLE',
       seed.is_recommend,
       seed.sort_order,
       current_date - seed.started_days,
       current_date - seed.ended_days,
       now() - seed.review_age - interval '6 hours',
       reviewer.id,
       now() - seed.review_age,
       author.id,
       author.id
from (
    values
        ('个性化标签云实验台', 'personalized-tag-cloud-lab', '按用户最近文章访问生成首页标签推荐，兼顾游客随机发现。', '/uploads/project/2026/06/personalized-tag-cloud-lab.webp', 'CONTENT_SYSTEM', '["Vue 3", "Spring Boot 3.5", "PostgreSQL", "Flyway", "Design Tokens"]', 'https://github.com/SakuraCianna/CreatorSpace', 'https://preview.creatorspace.local/tag-cloud', null, '## 作品说明\n\n这个实验台把访问日志、文章标签和全站热度混成一个轻量推荐模型。', true, 30, 28, 2, interval '2 days'),
        ('后台数据巡检面板', 'admin-data-audit-dashboard', '用真实运营视角观察后台列表、日志和统计卡片的可读性。', '/uploads/project/2026/06/admin-data-audit-dashboard.webp', 'CONTENT_SYSTEM', '["Vue 3", "ECharts", "Spring Boot", "PostgreSQL"]', 'https://github.com/SakuraCianna/CreatorSpace', 'https://preview.creatorspace.local/admin-audit', null, '## 作品说明\n\n面板关注审核队列、互动趋势和后台操作路径。', true, 40, 35, 5, interval '4 days'),
        ('响应式作品档案页', 'responsive-project-archive', '展示作品截图、里程碑、资源链接和过程记录。', '/uploads/project/2026/06/responsive-project-archive.webp', 'VISUAL_LAB', '["Vue 3", "CSS Grid", "Image Gallery", "Responsive Layout"]', 'https://github.com/SakuraCianna/CreatorSpace', 'https://preview.creatorspace.local/project-archive', null, '## 作品说明\n\n作品详情需要像展品档案，而不是普通详情页。', false, 50, 42, 9, interval '6 days')
) as seed(title, slug, description, cover_url, project_type, tech_stack, github_url, demo_url, video_url, content_markdown, is_recommend, sort_order, started_days, ended_days, review_age)
join users author on author.username = 'creator_lyra'
left join users reviewer on reviewer.username = '${adminUsername}'
on conflict (slug) do update
set description = excluded.description,
    cover_url = excluded.cover_url,
    project_type = excluded.project_type,
    tech_stack = excluded.tech_stack,
    content_markdown = excluded.content_markdown,
    status = excluded.status,
    is_recommend = excluded.is_recommend,
    sort_order = excluded.sort_order,
    updated_at = now();

insert into project_tags (project_id, tag_id)
select project.id, tag.id
from (
    values
        ('personalized-tag-cloud-lab', 'recommendation'),
        ('personalized-tag-cloud-lab', 'frontend-experience'),
        ('personalized-tag-cloud-lab', 'theme-blog'),
        ('admin-data-audit-dashboard', 'admin-workbench'),
        ('admin-data-audit-dashboard', 'data-governance'),
        ('admin-data-audit-dashboard', 'flyway'),
        ('responsive-project-archive', 'responsive-layout'),
        ('responsive-project-archive', 'image-gallery'),
        ('responsive-project-archive', 'readability')
) as seed(project_slug, tag_slug)
join portfolio_projects project on project.slug = seed.project_slug
join tags tag on tag.slug = seed.tag_slug
on conflict (project_id, tag_id) do nothing;

insert into project_images (project_id, image_url, caption, sort_order)
select project.id, seed.image_url, seed.caption, seed.sort_order
from (
    values
        ('personalized-tag-cloud-lab', '/uploads/project/2026/06/tag-cloud-overview.webp', '首页推荐标签云整体状态', 10),
        ('personalized-tag-cloud-lab', '/uploads/project/2026/06/tag-cloud-mobile.webp', '移动端标签换行和触控区域', 20),
        ('admin-data-audit-dashboard', '/uploads/project/2026/06/audit-dashboard-list.webp', '后台日志列表的审阅状态', 10),
        ('responsive-project-archive', '/uploads/project/2026/06/project-archive-gallery.webp', '作品档案截图画廊', 10)
) as seed(project_slug, image_url, caption, sort_order)
join portfolio_projects project on project.slug = seed.project_slug
where not exists (
    select 1 from project_images image
    where image.project_id = project.id and image.image_url = seed.image_url
);

insert into project_milestones (project_id, title, description, milestone_date, sort_order)
select project.id, seed.title, seed.description, current_date - seed.days_ago, seed.sort_order
from (
    values
        ('personalized-tag-cloud-lab', '接入访问日志', '文章详情访问写入 user_id 和目标文章。', 11, 10),
        ('personalized-tag-cloud-lab', '首页展示推荐结果', '前端优先读取推荐接口，失败回落全站标签。', 3, 20),
        ('admin-data-audit-dashboard', '梳理运营巡检路径', '串联审核队列、互动趋势、访问统计和操作日志。', 5, 10),
        ('responsive-project-archive', '整理多图展示', '为作品详情准备画廊和过程记录数据。', 8, 10)
) as seed(project_slug, title, description, days_ago, sort_order)
join portfolio_projects project on project.slug = seed.project_slug
where not exists (
    select 1 from project_milestones milestone
    where milestone.project_id = project.id and milestone.title = seed.title
);

insert into project_links (project_id, link_type, label, url, safe_status, sort_order)
select project.id, seed.link_type, seed.label, seed.url, 'SAFE', seed.sort_order
from (
    values
        ('personalized-tag-cloud-lab', 'OTHER', '推荐接口预览', 'https://preview.creatorspace.local/tag-cloud', 10),
        ('personalized-tag-cloud-lab', 'DOC', '算法说明', 'https://example.com/docs/recommendation', 20),
        ('admin-data-audit-dashboard', 'OTHER', '后台巡检预览', 'https://preview.creatorspace.local/admin-audit', 10),
        ('responsive-project-archive', 'OTHER', '作品档案预览', 'https://preview.creatorspace.local/project-archive', 10)
) as seed(project_slug, link_type, label, url, sort_order)
join portfolio_projects project on project.slug = seed.project_slug
where not exists (
    select 1 from project_links link
    where link.project_id = project.id and link.url = seed.url
);

insert into project_process_notes (project_id, phase, title, body, sort_order)
select project.id, seed.phase, seed.title, seed.body, seed.sort_order
from (
    values
        ('personalized-tag-cloud-lab', 'model', '偏好分层', '登录用户看自己的最近文章标签，游客看 IP 近 7 天和全站热度。', 10),
        ('personalized-tag-cloud-lab', 'ui', '标签可读性', '标签保留足够对比度，不再出现 hover 后文字消失。', 20),
        ('admin-data-audit-dashboard', 'data', '运营巡检视角', '聚合审核、互动、访问和日志线索，方便管理员快速定位运营重点。', 10),
        ('responsive-project-archive', 'gallery', '多图档案', '用截图、里程碑和资源链接构成作品上下文。', 10)
) as seed(project_slug, phase, title, body, sort_order)
join portfolio_projects project on project.slug = seed.project_slug
where not exists (
    select 1 from project_process_notes note
    where note.project_id = project.id and note.title = seed.title
);

insert into inspiration_cards (title, content, image_url, card_type, source_url, color, is_public, sort_order, created_by)
select seed.title, seed.content, seed.image_url, seed.card_type, seed.source_url, seed.color, true, seed.sort_order, u.id
from (
    values
        ('推荐标签云的随机扰动', '游客需要一点新鲜感，但不能完全随机，所以用权重、引用次数和 random 组合。', null, 'TEXT', null, '#315bff', 30),
        ('移动端标签换行参考', '胶囊标签需要稳定高度、清晰文字和足够点击面积。', '/uploads/inspiration/2026/06/tag-cloud-mobile-reference.webp', 'IMAGE', null, '#0f766e', 40),
        ('访问日志推荐 SQL', 'select tag_id, sum(score) from visit_logs join article_tags ... group by tag_id', null, 'CODE', null, '#7c3aed', 50),
        ('内容路径的主页入口', '标签云不是装饰，它应该引导读者进入主题博客的阅读路径。', null, 'PROMPT', 'https://example.com/inspiration/tag-cloud-entry', '#db2777', 60)
) as seed(title, content, image_url, card_type, source_url, color, sort_order)
join users u on u.username = 'creator_lyra'
where not exists (
    select 1 from inspiration_cards card
    where card.title = seed.title
);

insert into inspiration_tags (inspiration_id, tag_id)
select inspiration.id, tag.id
from (
    values
        ('推荐标签云的随机扰动', 'recommendation'),
        ('移动端标签换行参考', 'responsive-layout'),
        ('移动端标签换行参考', 'readability'),
        ('访问日志推荐 SQL', 'spring-security'),
        ('访问日志推荐 SQL', 'recommendation'),
        ('内容路径的主页入口', 'theme-blog'),
        ('内容路径的主页入口', 'reading-path')
) as seed(inspiration_title, tag_slug)
join inspiration_cards inspiration on inspiration.title = seed.inspiration_title
join tags tag on tag.slug = seed.tag_slug
on conflict (inspiration_id, tag_id) do nothing;

insert into inspiration_sources (inspiration_id, source_type, title, url, author, captured_at)
select inspiration.id, 'URL', '首页推荐标签讨论', 'https://example.com/creator-space/tag-recommendation', 'CreatorSpace Team', now() - interval '2 days'
from inspiration_cards inspiration
where inspiration.title = '推荐标签云的随机扰动'
  and not exists (
      select 1 from inspiration_sources source
      where source.inspiration_id = inspiration.id and source.url = 'https://example.com/creator-space/tag-recommendation'
  );

insert into inspiration_relations (inspiration_id, target_type, target_id, relation_type)
select inspiration.id, 'ARTICLE', article.id, 'SEED'
from inspiration_cards inspiration
join articles article on article.slug = 'homepage-tag-recommendation'
where inspiration.title in ('推荐标签云的随机扰动', '访问日志推荐 SQL')
  and not exists (
      select 1 from inspiration_relations relation
      where relation.inspiration_id = inspiration.id and relation.target_type = 'ARTICLE' and relation.target_id = article.id
  );

insert into comments (target_type, target_id, user_id, content, status, depth, like_count, ip_address, user_agent)
select seed.target_type, seed.target_id, seed.user_id, seed.content, seed.status, 0, seed.like_count, seed.ip_address::inet, 'CreatorSpace Browser'
from (
    select 'ARTICLE' as target_type, article.id as target_id, user_row.id as user_id,
           '这个推荐逻辑很适合主页，游客也能看到一点变化。' as content, 'APPROVED' as status, 8 as like_count, '127.0.6.10' as ip_address
    from articles article, users user_row
    where article.slug = 'homepage-tag-recommendation' and user_row.username = 'reader_frontend'
    union all
    select 'ARTICLE', article.id, user_row.id,
           '后台巡检面板把审核、访问和互动串在一起，看问题更快了。', 'APPROVED', 5, '127.0.6.11'
    from articles article, users user_row
    where article.slug = 'content-governance-field-notes' and user_row.username = 'reader_backend'
    union all
    select 'PROJECT', project.id, user_row.id,
           '截图画廊和里程碑可以继续加强，适合做作品档案。', 'PENDING', 2, '127.0.6.12'
    from portfolio_projects project, users user_row
    where project.slug = 'responsive-project-archive' and user_row.username = 'reader_visual'
) seed
where not exists (
    select 1 from comments comment_row
    where comment_row.target_type = seed.target_type
      and comment_row.target_id = seed.target_id
      and comment_row.user_id = seed.user_id
      and comment_row.content = seed.content
);

insert into like_records (target_type, target_id, user_id)
select seed.target_type, seed.target_id, seed.user_id
from (
    select 'ARTICLE' as target_type, article.id as target_id, user_row.id as user_id
    from articles article
    join users user_row on user_row.username in ('reader_frontend', 'reader_writer')
    where article.slug in ('homepage-tag-recommendation', 'theme-blog-reading-path-design')
    union all
    select 'PROJECT', project.id, user_row.id
    from portfolio_projects project
    join users user_row on user_row.username in ('reader_backend', 'reader_visual')
    where project.slug in ('personalized-tag-cloud-lab', 'admin-data-audit-dashboard')
    union all
    select 'INSPIRATION', inspiration.id, user_row.id
    from inspiration_cards inspiration
    join users user_row on user_row.username in ('reader_frontend', 'reader_visual')
    where inspiration.title in ('推荐标签云的随机扰动', '移动端标签换行参考')
) seed
on conflict (target_type, target_id, user_id) do nothing;

insert into favorite_records (target_type, target_id, user_id)
select seed.target_type, seed.target_id, seed.user_id
from (
    select 'ARTICLE' as target_type, article.id as target_id, user_row.id as user_id
    from articles article
    join users user_row on user_row.username in ('reader_frontend', 'reader_backend')
    where article.slug in ('homepage-tag-recommendation', 'security-public-recommendation-api')
    union all
    select 'PROJECT', project.id, user_row.id
    from portfolio_projects project
    join users user_row on user_row.username in ('reader_visual', 'reader_writer')
    where project.slug in ('responsive-project-archive', 'personalized-tag-cloud-lab')
    union all
    select 'INSPIRATION', inspiration.id, user_row.id
    from inspiration_cards inspiration
    join users user_row on user_row.username in ('reader_writer', 'reader_visual')
    where inspiration.title in ('内容路径的主页入口', '访问日志推荐 SQL')
) seed
on conflict (target_type, target_id, user_id) do nothing;

insert into visit_logs (path, target_type, target_id, user_id, ip_address, user_agent, referer, device_type, browser, operating_system, created_at)
select '/articles/' || article.slug,
       'ARTICLE',
       article.id,
       user_row.id,
       seed.ip_address::inet,
       'CreatorSpace Browser',
       '/',
       seed.device_type,
       'Chrome',
       'Windows',
       now() - (seed.hours_ago || ' hours')::interval
from (
    values
        ('reader_frontend', 'homepage-tag-recommendation', '127.0.7.10', 'DESKTOP', 2),
        ('reader_frontend', 'unified-form-controls', '127.0.7.10', 'DESKTOP', 6),
        ('reader_frontend', 'theme-blog-reading-path-design', '127.0.7.10', 'MOBILE', 20),
        ('reader_backend', 'security-public-recommendation-api', '127.0.7.11', 'DESKTOP', 3),
        ('reader_backend', 'content-governance-field-notes', '127.0.7.11', 'DESKTOP', 8),
        ('reader_visual', 'inspiration-wall-relations', '127.0.7.12', 'TABLET', 5),
        ('reader_visual', 'unified-form-controls', '127.0.7.12', 'TABLET', 16),
        ('reader_writer', 'theme-blog-reading-path-design', '127.0.7.13', 'MOBILE', 4),
        ('reader_writer', 'homepage-tag-recommendation', '127.0.7.13', 'MOBILE', 12)
) as seed(username, article_slug, ip_address, device_type, hours_ago)
join users user_row on user_row.username = seed.username
join articles article on article.slug = seed.article_slug
where not exists (
    select 1 from visit_logs log_row
    where log_row.user_id = user_row.id
      and log_row.target_type = 'ARTICLE'
      and log_row.target_id = article.id
      and log_row.created_at >= now() - interval '36 hours'
);

insert into content_statistics (target_type, target_id, view_count, like_count, favorite_count, comment_count, last_viewed_at)
select 'ARTICLE', article.id, article.view_count, article.like_count, 12, article.comment_count, now() - interval '2 hours'
from articles article
where article.slug in (
    'homepage-tag-recommendation',
    'unified-form-controls',
    'security-public-recommendation-api',
    'content-governance-field-notes',
    'theme-blog-reading-path-design',
    'inspiration-wall-relations'
)
on conflict (target_type, target_id) do update
set view_count = greatest(content_statistics.view_count, excluded.view_count),
    like_count = greatest(content_statistics.like_count, excluded.like_count),
    favorite_count = greatest(content_statistics.favorite_count, excluded.favorite_count),
    comment_count = greatest(content_statistics.comment_count, excluded.comment_count),
    last_viewed_at = greatest(content_statistics.last_viewed_at, excluded.last_viewed_at),
    updated_at = now();

insert into search_logs (keyword, result_count, user_id, ip_address, user_agent, created_at)
select seed.keyword, seed.result_count, user_row.id, seed.ip_address::inet, 'CreatorSpace Browser', now() - seed.age
from (
    values
        ('推荐标签', 8, 'reader_frontend', '127.0.8.10', interval '3 hours'),
        ('响应式布局', 6, 'reader_frontend', '127.0.8.10', interval '8 hours'),
        ('Flyway 数据', 5, 'reader_backend', '127.0.8.11', interval '1 day'),
        ('灵感墙 关联', 7, 'reader_visual', '127.0.8.12', interval '2 days'),
        ('主题博客 阅读路径', 9, 'reader_writer', '127.0.8.13', interval '3 days')
) as seed(keyword, result_count, username, ip_address, age)
join users user_row on user_row.username = seed.username
where not exists (
    select 1 from search_logs log_row
    where log_row.keyword = seed.keyword
      and log_row.user_id = user_row.id
      and log_row.created_at >= now() - interval '7 days'
);

insert into daily_metrics (metric_date, metric_key, metric_value, detail_json)
values
    (current_date, 'tag.recommendation.request', 48, '{"source": "content-ops", "source": "homepage"}'::jsonb),
    (current_date, 'tag.recommendation.guest', 21, '{"source": "content-ops", "mode": "visitor"}'::jsonb),
    (current_date, 'tag.recommendation.member', 27, '{"source": "content-ops", "mode": "user"}'::jsonb),
    (current_date - 1, 'content.governance.coverage', 52, '{"source": "content-ops", "tables": "core-business"}'::jsonb)
on conflict (metric_date, metric_key) do update
set metric_value = excluded.metric_value,
    detail_json = excluded.detail_json,
    updated_at = now();

insert into operation_logs (operator_id, operation, module, target_type, target_id, request_method, request_path, detail_json)
select admin_user.id,
       seed.operation,
       seed.module,
       seed.target_type,
       seed.target_id,
       seed.request_method,
       seed.request_path,
       seed.detail_json::jsonb
from (
    select '刷新推荐标签策略' as operation, 'TAG' as module, 'TAG' as target_type, tag.id as target_id,
           'GET' as request_method, '/api/tags/recommended' as request_path,
           '{"source": "content-ops", "mode": "recommendation"}' as detail_json
    from tags tag where tag.slug = 'recommendation'
    union all
    select '整理作品档案资料', 'PROJECT', 'PROJECT', project.id,
           'POST', '/api/admin/projects', '{"source": "content-ops", "scope": "project-archive"}'
    from portfolio_projects project where project.slug = 'responsive-project-archive'
) seed
left join users admin_user on admin_user.username = '${adminUsername}'
where not exists (
    select 1 from operation_logs log_row
    where log_row.operation = seed.operation
      and log_row.target_type = seed.target_type
      and log_row.target_id = seed.target_id
      and log_row.created_at >= now() - interval '7 days'
);

insert into guestbook_entries (user_id, display_name, content, status, like_count, ip_address, user_agent)
select user_row.id, user_row.nickname, seed.content, 'APPROVED', seed.like_count, seed.ip_address::inet, 'CreatorSpace Browser'
from (
    values
        ('reader_frontend', '首页推荐标签终于不是固定排序了，视觉也更像一个有生命的入口。', 10, '127.0.9.10'),
        ('reader_backend', '运营记录让后台统计和日志页面更容易观察。', 7, '127.0.9.11'),
        ('reader_writer', '主题博客的标签云可以继续接到系列页和阅读路径。', 9, '127.0.9.12')
) as seed(username, content, like_count, ip_address)
join users user_row on user_row.username = seed.username
where not exists (
    select 1 from guestbook_entries entry
    where entry.user_id = user_row.id and entry.content = seed.content
);

insert into friend_links (site_name, site_url, logo_url, description, status, sort_order)
values
    ('Frontend Garden', 'https://journal.creatorspace.local/frontend-garden', '/uploads/avatars/avatar-reader-frontend.webp', '收集前端体验和设计系统笔记。', 'APPROVED', 20),
    ('Data Governance Lab', 'https://journal.creatorspace.local/data-governance-lab', '/uploads/avatars/avatar-reader-backend.webp', '内容治理、后台巡检和质量观察案例。', 'APPROVED', 30)
on conflict do nothing;

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
select seed.owner_type, null, seed.block_key, seed.block_type, seed.title, seed.body, seed.config_json::jsonb, seed.sort_order, seed.visible
from (
    values
        ('HOME', 'ops.home-tags', 'CUSTOM', '首页推荐标签', '把近期阅读偏好转成可点击的主题入口，让首页更像个人化起点。', '{"surface": "HOME", "topic": "home-tags", "responsive": true}', 100, true),
        ('HOME', 'ops.form-controls', 'TEXT', '表单控件体验', '记录下拉框、上传按钮和输入态的统一规范，减少页面之间的割裂感。', '{"surface": "HOME", "topic": "form-controls", "responsive": true}', 110, true),
        ('HOME', 'ops.theme-reading', 'MARKDOWN', '主题博客阅读', '把文章系列、标签和关联内容串起来，帮助读者沿着主题继续阅读。', '{"surface": "HOME", "topic": "theme-reading", "responsive": true}', 120, true),
        ('HOME', 'ops.project-gallery', 'GALLERY', '作品档案截图', '用封面、截图和过程图说明作品演进，而不是只留下一个链接。', '{"surface": "HOME", "topic": "project-gallery", "responsive": true}', 130, true),
        ('HOME', 'ops.inspiration-links', 'TIMELINE', '灵感墙关联', '让灵感卡片能回到文章和作品，保留创作上下文。', '{"surface": "HOME", "topic": "inspiration-links", "responsive": true}', 140, true),
        ('HOME', 'ops.admin-list', 'CUSTOM', '后台运营列表', '关注审核队列、评论状态和资源归档，方便管理员快速判断优先级。', '{"surface": "HOME", "topic": "admin-list", "responsive": true}', 150, true),
        ('HOME', 'ops.search-rank', 'TEXT', '搜索结果排序', '结合标题、摘要、标签和访问记录，让搜索结果更贴近读者意图。', '{"surface": "HOME", "topic": "search-rank", "responsive": true}', 160, true),
        ('HOME', 'ops.mobile-filter', 'MARKDOWN', '移动端筛选', '在窄屏下保留清晰的筛选层级，避免标签和按钮挤在一起。', '{"surface": "HOME", "topic": "mobile-filter", "responsive": true}', 170, true),
        ('ARTICLE', 'ops.review-flow', 'GALLERY', '内容审核流', '把提交、审核、发布和撤回状态放进同一条运营链路。', '{"surface": "ARTICLE", "topic": "review-flow", "responsive": true}', 180, true),
        ('ARTICLE', 'ops.visit-metrics', 'TIMELINE', '访问统计口径', '区分访客访问、登录阅读和搜索进入，避免统计口径混乱。', '{"surface": "ARTICLE", "topic": "visit-metrics", "responsive": true}', 190, true),
        ('ARTICLE', 'ops.tag-alias', 'CUSTOM', '标签别名整理', '把相近写法合并到稳定标签，减少内容归档时的噪音。', '{"surface": "ARTICLE", "topic": "tag-alias", "responsive": true}', 200, true),
        ('ARTICLE', 'ops.cover-rule', 'TEXT', '封面图选择', '根据内容类型选择封面比例和说明文字，让列表扫描更顺畅。', '{"surface": "ARTICLE", "topic": "cover-rule", "responsive": true}', 210, true),
        ('ARTICLE', 'ops.comment-review', 'MARKDOWN', '评论审核提示', '把待审评论、上下文和处理结果放在一起，减少误判。', '{"surface": "ARTICLE", "topic": "comment-review", "responsive": true}', 220, true),
        ('PROJECT', 'ops.file-archive', 'GALLERY', '文件资源归档', '给截图、参考图和附件保留来源说明，方便后续维护。', '{"surface": "PROJECT", "topic": "file-archive", "responsive": true}', 230, true),
        ('PROJECT', 'ops.theme-preview', 'TIMELINE', '主题配置预览', '记录主题切换前后的页面表现，避免颜色和文字对比失衡。', '{"surface": "PROJECT", "topic": "theme-preview", "responsive": true}', 240, true),
        ('PROJECT', 'ops.ai-review', 'CUSTOM', 'AI 审核建议', '把自动摘要和人工判断分开展示，让建议可追溯也可忽略。', '{"surface": "PROJECT", "topic": "ai-review", "responsive": true}', 250, true),
        ('PROJECT', 'ops.public-api', 'TEXT', '公开接口边界', '明确游客、普通用户和管理员各自能看到的内容范围。', '{"surface": "PROJECT", "topic": "public-api", "responsive": true}', 260, true),
        ('ABOUT', 'ops.favorite-path', 'MARKDOWN', '用户收藏路径', '观察收藏、回访和继续阅读之间的关系，完善内容入口。', '{"surface": "ABOUT", "topic": "favorite-path", "responsive": true}', 270, true),
        ('ABOUT', 'ops.guestbook-care', 'GALLERY', '留言板维护', '保留读者反馈的处理状态，让公开留言不丢上下文。', '{"surface": "ABOUT", "topic": "guestbook-care", "responsive": true}', 280, true),
        ('ABOUT', 'ops.process-note', 'TIMELINE', '项目过程记录', '把设计选择、实现调整和上线后的观察放进同一条时间线。', '{"surface": "ABOUT", "topic": "process-note", "responsive": true}', 290, true)
) as seed(owner_type, block_key, block_type, title, body, config_json, sort_order, visible)
where not exists (select 1 from content_blocks block where block.block_key = seed.block_key);

insert into search_logs (keyword, result_count, user_id, ip_address, user_agent, created_at)
select seed.keyword, seed.result_count, user_row.id, seed.ip_address::inet, seed.user_agent, now() - seed.age
from (
    values
        ('首页推荐标签 怎么做', 3, 'mira_frontend', '127.0.11.20', 'CreatorSpace Chrome', interval '2 hours'),
        ('表单控件体验 最佳实践', 8, 'zhou_backend', '127.0.11.21', 'CreatorSpace Edge', interval '3 hours'),
        ('主题博客阅读 CreatorSpace', 13, 'lin_visual', '127.0.11.22', 'CreatorSpace Mobile Safari', interval '4 hours'),
        ('作品档案截图 设计细节', 18, 'qiao_writer', '127.0.11.23', 'CreatorSpace Chrome', interval '5 hours'),
        ('灵感墙关联 怎么做', 6, 'chen_product', '127.0.11.24', 'CreatorSpace Edge', interval '6 hours'),
        ('后台运营列表 最佳实践', 11, 'he_reader', '127.0.11.25', 'CreatorSpace Mobile Safari', interval '7 hours'),
        ('搜索结果排序 CreatorSpace', 16, 'mira_frontend', '127.0.11.26', 'CreatorSpace Chrome', interval '8 hours'),
        ('移动端筛选 设计细节', 4, 'zhou_backend', '127.0.11.27', 'CreatorSpace Edge', interval '9 hours'),
        ('内容审核流 怎么做', 9, 'lin_visual', '127.0.11.28', 'CreatorSpace Mobile Safari', interval '10 hours'),
        ('访问统计口径 最佳实践', 14, 'qiao_writer', '127.0.11.29', 'CreatorSpace Chrome', interval '11 hours'),
        ('标签别名整理 CreatorSpace', 19, 'chen_product', '127.0.11.30', 'CreatorSpace Edge', interval '12 hours'),
        ('封面图选择 设计细节', 7, 'he_reader', '127.0.11.31', 'CreatorSpace Mobile Safari', interval '13 hours'),
        ('评论审核提示 怎么做', 12, 'mira_frontend', '127.0.11.32', 'CreatorSpace Chrome', interval '14 hours'),
        ('文件资源归档 最佳实践', 17, 'zhou_backend', '127.0.11.33', 'CreatorSpace Edge', interval '15 hours'),
        ('主题配置预览 CreatorSpace', 5, 'lin_visual', '127.0.11.34', 'CreatorSpace Mobile Safari', interval '16 hours'),
        ('AI 审核建议 设计细节', 10, 'qiao_writer', '127.0.11.35', 'CreatorSpace Chrome', interval '17 hours'),
        ('公开接口边界 怎么做', 15, 'chen_product', '127.0.11.36', 'CreatorSpace Edge', interval '18 hours'),
        ('用户收藏路径 最佳实践', 3, 'he_reader', '127.0.11.37', 'CreatorSpace Mobile Safari', interval '19 hours'),
        ('留言板维护 CreatorSpace', 8, 'mira_frontend', '127.0.11.38', 'CreatorSpace Chrome', interval '20 hours'),
        ('项目过程记录 设计细节', 13, 'zhou_backend', '127.0.11.39', 'CreatorSpace Edge', interval '21 hours'),
        ('首页推荐标签 怎么做', 18, 'lin_visual', '127.0.11.40', 'CreatorSpace Mobile Safari', interval '22 hours'),
        ('表单控件体验 最佳实践', 6, 'qiao_writer', '127.0.11.41', 'CreatorSpace Chrome', interval '23 hours'),
        ('主题博客阅读 CreatorSpace', 11, 'chen_product', '127.0.11.42', 'CreatorSpace Edge', interval '24 hours'),
        ('作品档案截图 设计细节', 16, 'he_reader', '127.0.11.43', 'CreatorSpace Mobile Safari', interval '25 hours'),
        ('灵感墙关联 怎么做', 4, 'mira_frontend', '127.0.11.44', 'CreatorSpace Chrome', interval '26 hours'),
        ('后台运营列表 最佳实践', 9, 'zhou_backend', '127.0.11.45', 'CreatorSpace Edge', interval '27 hours'),
        ('搜索结果排序 CreatorSpace', 14, 'lin_visual', '127.0.11.46', 'CreatorSpace Mobile Safari', interval '28 hours'),
        ('移动端筛选 设计细节', 19, 'qiao_writer', '127.0.11.47', 'CreatorSpace Chrome', interval '29 hours'),
        ('内容审核流 怎么做', 7, 'chen_product', '127.0.11.48', 'CreatorSpace Edge', interval '30 hours'),
        ('访问统计口径 最佳实践', 12, 'he_reader', '127.0.11.49', 'CreatorSpace Mobile Safari', interval '31 hours'),
        ('标签别名整理 CreatorSpace', 17, 'mira_frontend', '127.0.11.50', 'CreatorSpace Chrome', interval '32 hours'),
        ('封面图选择 设计细节', 5, 'zhou_backend', '127.0.11.51', 'CreatorSpace Edge', interval '33 hours'),
        ('评论审核提示 怎么做', 10, 'lin_visual', '127.0.11.52', 'CreatorSpace Mobile Safari', interval '34 hours'),
        ('文件资源归档 最佳实践', 15, 'qiao_writer', '127.0.11.53', 'CreatorSpace Chrome', interval '35 hours'),
        ('主题配置预览 CreatorSpace', 3, 'chen_product', '127.0.11.54', 'CreatorSpace Edge', interval '36 hours'),
        ('AI 审核建议 设计细节', 8, 'he_reader', '127.0.11.55', 'CreatorSpace Mobile Safari', interval '37 hours')
) as seed(keyword, result_count, username, ip_address, user_agent, age)
join users user_row on user_row.username = seed.username
where not exists (select 1 from search_logs log_row where log_row.keyword = seed.keyword and log_row.user_id = user_row.id and log_row.created_at >= now() - interval '30 days');

insert into operation_logs (operator_id, operation, module, target_type, target_id, request_method, request_path, ip_address, user_agent, detail_json, created_at)
select admin_user.id, seed.operation, seed.module, seed.target_type, null, seed.method, seed.path, seed.ip_address::inet, seed.user_agent, seed.detail_json::jsonb, now() - seed.age
from (
    values
        ('调整公开状态 - 首页推荐标签', 'ARTICLE', 'ARTICLE', 'GET', '/api/admin/article/review-1', '127.0.12.30', 'CreatorSpace Admin Browser', '{"module": "ARTICLE", "topic": "首页推荐标签", "result": "ok"}', interval '3 hours'),
        ('更新展示信息 - 表单控件体验', 'PROJECT', 'PROJECT', 'POST', '/api/admin/project/review-2', '127.0.12.31', 'CreatorSpace Admin Browser', '{"module": "PROJECT", "topic": "表单控件体验", "result": "ok"}', interval '4 hours'),
        ('检查关联数据 - 主题博客阅读', 'COMMENT', 'COMMENT', 'PUT', '/api/admin/comment/review-3', '127.0.12.32', 'CreatorSpace Admin Browser', '{"module": "COMMENT", "topic": "主题博客阅读", "result": "ok"}', interval '5 hours'),
        ('整理运营备注 - 作品档案截图', 'FILE', 'FILE', 'GET', '/api/admin/file/review-4', '127.0.12.33', 'CreatorSpace Admin Browser', '{"module": "FILE", "topic": "作品档案截图", "result": "ok"}', interval '6 hours'),
        ('同步页面配置 - 灵感墙关联', 'THEME', 'THEME', 'POST', '/api/admin/theme/review-5', '127.0.12.34', 'CreatorSpace Admin Browser', '{"module": "THEME", "topic": "灵感墙关联", "result": "ok"}', interval '7 hours'),
        ('调整公开状态 - 后台运营列表', 'SITE', 'SITE', 'PUT', '/api/admin/site/review-6', '127.0.12.35', 'CreatorSpace Admin Browser', '{"module": "SITE", "topic": "后台运营列表", "result": "ok"}', interval '8 hours'),
        ('更新展示信息 - 搜索结果排序', 'INSPIRATION', 'INSPIRATION', 'GET', '/api/admin/inspiration/review-7', '127.0.12.36', 'CreatorSpace Admin Browser', '{"module": "INSPIRATION", "topic": "搜索结果排序", "result": "ok"}', interval '9 hours'),
        ('检查关联数据 - 移动端筛选', 'GUESTBOOK', 'GUESTBOOK', 'POST', '/api/admin/guestbook/review-8', '127.0.12.37', 'CreatorSpace Admin Browser', '{"module": "GUESTBOOK", "topic": "移动端筛选", "result": "ok"}', interval '10 hours'),
        ('整理运营备注 - 内容审核流', 'CATEGORY', 'CATEGORY', 'PUT', '/api/admin/category/review-9', '127.0.12.38', 'CreatorSpace Admin Browser', '{"module": "CATEGORY", "topic": "内容审核流", "result": "ok"}', interval '11 hours'),
        ('同步页面配置 - 访问统计口径', 'TAG', 'TAG', 'GET', '/api/admin/tag/review-10', '127.0.12.39', 'CreatorSpace Admin Browser', '{"module": "TAG", "topic": "访问统计口径", "result": "ok"}', interval '12 hours'),
        ('调整公开状态 - 标签别名整理', 'ADMIN', 'ADMIN', 'POST', '/api/admin/admin/review-11', '127.0.12.40', 'CreatorSpace Admin Browser', '{"module": "ADMIN", "topic": "标签别名整理", "result": "ok"}', interval '13 hours'),
        ('更新展示信息 - 封面图选择', 'ARTICLE', 'ARTICLE', 'PUT', '/api/admin/article/review-12', '127.0.12.41', 'CreatorSpace Admin Browser', '{"module": "ARTICLE", "topic": "封面图选择", "result": "ok"}', interval '14 hours'),
        ('检查关联数据 - 评论审核提示', 'PROJECT', 'PROJECT', 'GET', '/api/admin/project/review-13', '127.0.12.42', 'CreatorSpace Admin Browser', '{"module": "PROJECT", "topic": "评论审核提示", "result": "ok"}', interval '15 hours'),
        ('整理运营备注 - 文件资源归档', 'COMMENT', 'COMMENT', 'POST', '/api/admin/comment/review-14', '127.0.12.43', 'CreatorSpace Admin Browser', '{"module": "COMMENT", "topic": "文件资源归档", "result": "ok"}', interval '16 hours'),
        ('同步页面配置 - 主题配置预览', 'FILE', 'FILE', 'PUT', '/api/admin/file/review-15', '127.0.12.44', 'CreatorSpace Admin Browser', '{"module": "FILE", "topic": "主题配置预览", "result": "ok"}', interval '17 hours'),
        ('调整公开状态 - AI 审核建议', 'THEME', 'THEME', 'GET', '/api/admin/theme/review-16', '127.0.12.45', 'CreatorSpace Admin Browser', '{"module": "THEME", "topic": "AI 审核建议", "result": "ok"}', interval '18 hours'),
        ('更新展示信息 - 公开接口边界', 'SITE', 'SITE', 'POST', '/api/admin/site/review-17', '127.0.12.46', 'CreatorSpace Admin Browser', '{"module": "SITE", "topic": "公开接口边界", "result": "ok"}', interval '19 hours'),
        ('检查关联数据 - 用户收藏路径', 'INSPIRATION', 'INSPIRATION', 'PUT', '/api/admin/inspiration/review-18', '127.0.12.47', 'CreatorSpace Admin Browser', '{"module": "INSPIRATION", "topic": "用户收藏路径", "result": "ok"}', interval '20 hours'),
        ('整理运营备注 - 留言板维护', 'GUESTBOOK', 'GUESTBOOK', 'GET', '/api/admin/guestbook/review-19', '127.0.12.48', 'CreatorSpace Admin Browser', '{"module": "GUESTBOOK", "topic": "留言板维护", "result": "ok"}', interval '21 hours'),
        ('同步页面配置 - 项目过程记录', 'CATEGORY', 'CATEGORY', 'POST', '/api/admin/category/review-20', '127.0.12.49', 'CreatorSpace Admin Browser', '{"module": "CATEGORY", "topic": "项目过程记录", "result": "ok"}', interval '22 hours'),
        ('调整公开状态 - 首页推荐标签', 'TAG', 'TAG', 'PUT', '/api/admin/tag/review-21', '127.0.12.50', 'CreatorSpace Admin Browser', '{"module": "TAG", "topic": "首页推荐标签", "result": "ok"}', interval '23 hours'),
        ('更新展示信息 - 表单控件体验', 'ADMIN', 'ADMIN', 'GET', '/api/admin/admin/review-22', '127.0.12.51', 'CreatorSpace Admin Browser', '{"module": "ADMIN", "topic": "表单控件体验", "result": "ok"}', interval '24 hours'),
        ('检查关联数据 - 主题博客阅读', 'ARTICLE', 'ARTICLE', 'POST', '/api/admin/article/review-23', '127.0.12.52', 'CreatorSpace Admin Browser', '{"module": "ARTICLE", "topic": "主题博客阅读", "result": "ok"}', interval '25 hours'),
        ('整理运营备注 - 作品档案截图', 'PROJECT', 'PROJECT', 'PUT', '/api/admin/project/review-24', '127.0.12.53', 'CreatorSpace Admin Browser', '{"module": "PROJECT", "topic": "作品档案截图", "result": "ok"}', interval '26 hours'),
        ('同步页面配置 - 灵感墙关联', 'COMMENT', 'COMMENT', 'GET', '/api/admin/comment/review-25', '127.0.12.54', 'CreatorSpace Admin Browser', '{"module": "COMMENT", "topic": "灵感墙关联", "result": "ok"}', interval '27 hours'),
        ('调整公开状态 - 后台运营列表', 'FILE', 'FILE', 'POST', '/api/admin/file/review-26', '127.0.12.55', 'CreatorSpace Admin Browser', '{"module": "FILE", "topic": "后台运营列表", "result": "ok"}', interval '28 hours'),
        ('更新展示信息 - 搜索结果排序', 'THEME', 'THEME', 'PUT', '/api/admin/theme/review-27', '127.0.12.56', 'CreatorSpace Admin Browser', '{"module": "THEME", "topic": "搜索结果排序", "result": "ok"}', interval '29 hours'),
        ('检查关联数据 - 移动端筛选', 'SITE', 'SITE', 'GET', '/api/admin/site/review-28', '127.0.12.57', 'CreatorSpace Admin Browser', '{"module": "SITE", "topic": "移动端筛选", "result": "ok"}', interval '30 hours'),
        ('整理运营备注 - 内容审核流', 'INSPIRATION', 'INSPIRATION', 'POST', '/api/admin/inspiration/review-29', '127.0.12.58', 'CreatorSpace Admin Browser', '{"module": "INSPIRATION", "topic": "内容审核流", "result": "ok"}', interval '31 hours'),
        ('同步页面配置 - 访问统计口径', 'GUESTBOOK', 'GUESTBOOK', 'PUT', '/api/admin/guestbook/review-30', '127.0.12.59', 'CreatorSpace Admin Browser', '{"module": "GUESTBOOK", "topic": "访问统计口径", "result": "ok"}', interval '32 hours'),
        ('调整公开状态 - 标签别名整理', 'CATEGORY', 'CATEGORY', 'GET', '/api/admin/category/review-31', '127.0.12.60', 'CreatorSpace Admin Browser', '{"module": "CATEGORY", "topic": "标签别名整理", "result": "ok"}', interval '33 hours'),
        ('更新展示信息 - 封面图选择', 'TAG', 'TAG', 'POST', '/api/admin/tag/review-32', '127.0.12.61', 'CreatorSpace Admin Browser', '{"module": "TAG", "topic": "封面图选择", "result": "ok"}', interval '34 hours'),
        ('检查关联数据 - 评论审核提示', 'ADMIN', 'ADMIN', 'PUT', '/api/admin/admin/review-33', '127.0.12.62', 'CreatorSpace Admin Browser', '{"module": "ADMIN", "topic": "评论审核提示", "result": "ok"}', interval '35 hours'),
        ('整理运营备注 - 文件资源归档', 'ARTICLE', 'ARTICLE', 'GET', '/api/admin/article/review-34', '127.0.12.63', 'CreatorSpace Admin Browser', '{"module": "ARTICLE", "topic": "文件资源归档", "result": "ok"}', interval '36 hours')
) as seed(operation, module, target_type, method, path, ip_address, user_agent, detail_json, age)
left join users admin_user on admin_user.id = (select u.id from users u join user_roles ur on ur.user_id = u.id join roles r on r.id = ur.role_id where r.code = 'ADMIN' order by u.id limit 1)
where not exists (select 1 from operation_logs log_row where log_row.operation = seed.operation and log_row.created_at >= now() - interval '30 days');

insert into daily_metrics (metric_date, metric_key, metric_value, detail_json)
values
    (current_date - 0, 'home.tag.click', 12, '{"channel": "creator-space", "topic": "首页推荐标签"}'::jsonb),
    (current_date - 1, 'article.read.depth', 19, '{"channel": "creator-space", "topic": "表单控件体验"}'::jsonb),
    (current_date - 2, 'project.archive.open', 26, '{"channel": "creator-space", "topic": "主题博客阅读"}'::jsonb),
    (current_date - 3, 'inspiration.relation.open', 33, '{"channel": "creator-space", "topic": "作品档案截图"}'::jsonb),
    (current_date - 4, 'admin.review.done', 40, '{"channel": "creator-space", "topic": "灵感墙关联"}'::jsonb),
    (current_date - 5, 'search.keyword.hit', 47, '{"channel": "creator-space", "topic": "后台运营列表"}'::jsonb),
    (current_date - 6, 'guestbook.approved', 54, '{"channel": "creator-space", "topic": "搜索结果排序"}'::jsonb),
    (current_date - 7, 'file.resource.used', 61, '{"channel": "creator-space", "topic": "移动端筛选"}'::jsonb),
    (current_date - 8, 'theme.preview.open', 68, '{"channel": "creator-space", "topic": "内容审核流"}'::jsonb),
    (current_date - 9, 'ai.suggestion.created', 75, '{"channel": "creator-space", "topic": "访问统计口径"}'::jsonb),
    (current_date - 10, 'home.tag.click', 82, '{"channel": "creator-space", "topic": "标签别名整理"}'::jsonb),
    (current_date - 11, 'article.read.depth', 89, '{"channel": "creator-space", "topic": "封面图选择"}'::jsonb),
    (current_date - 12, 'project.archive.open', 96, '{"channel": "creator-space", "topic": "评论审核提示"}'::jsonb),
    (current_date - 13, 'inspiration.relation.open', 13, '{"channel": "creator-space", "topic": "文件资源归档"}'::jsonb),
    (current_date - 14, 'admin.review.done', 20, '{"channel": "creator-space", "topic": "主题配置预览"}'::jsonb),
    (current_date - 15, 'search.keyword.hit', 27, '{"channel": "creator-space", "topic": "AI 审核建议"}'::jsonb),
    (current_date - 16, 'guestbook.approved', 34, '{"channel": "creator-space", "topic": "公开接口边界"}'::jsonb),
    (current_date - 17, 'file.resource.used', 41, '{"channel": "creator-space", "topic": "用户收藏路径"}'::jsonb),
    (current_date - 18, 'theme.preview.open', 48, '{"channel": "creator-space", "topic": "留言板维护"}'::jsonb),
    (current_date - 19, 'ai.suggestion.created', 55, '{"channel": "creator-space", "topic": "项目过程记录"}'::jsonb),
    (current_date - 20, 'home.tag.click', 62, '{"channel": "creator-space", "topic": "首页推荐标签"}'::jsonb),
    (current_date - 21, 'article.read.depth', 69, '{"channel": "creator-space", "topic": "表单控件体验"}'::jsonb),
    (current_date - 22, 'project.archive.open', 76, '{"channel": "creator-space", "topic": "主题博客阅读"}'::jsonb),
    (current_date - 23, 'inspiration.relation.open', 83, '{"channel": "creator-space", "topic": "作品档案截图"}'::jsonb),
    (current_date - 24, 'admin.review.done', 90, '{"channel": "creator-space", "topic": "灵感墙关联"}'::jsonb),
    (current_date - 25, 'search.keyword.hit', 97, '{"channel": "creator-space", "topic": "后台运营列表"}'::jsonb)
on conflict (metric_date, metric_key) do update set metric_value = excluded.metric_value, detail_json = excluded.detail_json, updated_at = now();

insert into ai_agent_tasks (task_type, target_type, target_id, prompt, status, provider, model_name, created_by)
select seed.task_type, seed.target_type, article.id, seed.prompt, seed.status, seed.provider, seed.model_name, user_row.id
from (
    values
        ('CONTENT_POLISH', 'ARTICLE', 'homepage-tag-recommendation', '请检查《首页推荐标签》相关内容的摘要、标签和公开展示语气。', 'PENDING', 'openai-compatible', 'content-reasoner', 'mira_frontend'),
        ('REVIEW_ASSIST', 'ARTICLE', 'unified-form-controls', '请检查《表单控件体验》相关内容的摘要、标签和公开展示语气。', 'RUNNING', 'openai-compatible', 'content-reasoner', 'zhou_backend'),
        ('SUMMARY_WRITE', 'ARTICLE', 'security-public-recommendation-api', '请检查《主题博客阅读》相关内容的摘要、标签和公开展示语气。', 'SUCCEEDED', 'openai-compatible', 'content-reasoner', 'lin_visual'),
        ('CONTENT_POLISH', 'ARTICLE', 'content-governance-field-notes', '请检查《作品档案截图》相关内容的摘要、标签和公开展示语气。', 'PENDING', 'openai-compatible', 'content-reasoner', 'qiao_writer'),
        ('REVIEW_ASSIST', 'ARTICLE', 'theme-blog-reading-path-design', '请检查《灵感墙关联》相关内容的摘要、标签和公开展示语气。', 'RUNNING', 'openai-compatible', 'content-reasoner', 'chen_product'),
        ('SUMMARY_WRITE', 'ARTICLE', 'inspiration-wall-relations', '请检查《后台运营列表》相关内容的摘要、标签和公开展示语气。', 'SUCCEEDED', 'openai-compatible', 'content-reasoner', 'he_reader'),
        ('CONTENT_POLISH', 'ARTICLE', 'homepage-tag-recommendation', '请检查《搜索结果排序》相关内容的摘要、标签和公开展示语气。', 'PENDING', 'openai-compatible', 'content-reasoner', 'mira_frontend'),
        ('REVIEW_ASSIST', 'ARTICLE', 'unified-form-controls', '请检查《移动端筛选》相关内容的摘要、标签和公开展示语气。', 'RUNNING', 'openai-compatible', 'content-reasoner', 'zhou_backend'),
        ('SUMMARY_WRITE', 'ARTICLE', 'security-public-recommendation-api', '请检查《内容审核流》相关内容的摘要、标签和公开展示语气。', 'SUCCEEDED', 'openai-compatible', 'content-reasoner', 'lin_visual'),
        ('CONTENT_POLISH', 'ARTICLE', 'content-governance-field-notes', '请检查《访问统计口径》相关内容的摘要、标签和公开展示语气。', 'PENDING', 'openai-compatible', 'content-reasoner', 'qiao_writer'),
        ('REVIEW_ASSIST', 'ARTICLE', 'theme-blog-reading-path-design', '请检查《标签别名整理》相关内容的摘要、标签和公开展示语气。', 'RUNNING', 'openai-compatible', 'content-reasoner', 'chen_product'),
        ('SUMMARY_WRITE', 'ARTICLE', 'inspiration-wall-relations', '请检查《封面图选择》相关内容的摘要、标签和公开展示语气。', 'SUCCEEDED', 'openai-compatible', 'content-reasoner', 'he_reader'),
        ('CONTENT_POLISH', 'ARTICLE', 'homepage-tag-recommendation', '请检查《评论审核提示》相关内容的摘要、标签和公开展示语气。', 'PENDING', 'openai-compatible', 'content-reasoner', 'mira_frontend'),
        ('REVIEW_ASSIST', 'ARTICLE', 'unified-form-controls', '请检查《文件资源归档》相关内容的摘要、标签和公开展示语气。', 'RUNNING', 'openai-compatible', 'content-reasoner', 'zhou_backend'),
        ('SUMMARY_WRITE', 'ARTICLE', 'security-public-recommendation-api', '请检查《主题配置预览》相关内容的摘要、标签和公开展示语气。', 'SUCCEEDED', 'openai-compatible', 'content-reasoner', 'lin_visual'),
        ('CONTENT_POLISH', 'ARTICLE', 'content-governance-field-notes', '请检查《AI 审核建议》相关内容的摘要、标签和公开展示语气。', 'PENDING', 'openai-compatible', 'content-reasoner', 'qiao_writer'),
        ('REVIEW_ASSIST', 'ARTICLE', 'theme-blog-reading-path-design', '请检查《公开接口边界》相关内容的摘要、标签和公开展示语气。', 'RUNNING', 'openai-compatible', 'content-reasoner', 'chen_product'),
        ('SUMMARY_WRITE', 'ARTICLE', 'inspiration-wall-relations', '请检查《用户收藏路径》相关内容的摘要、标签和公开展示语气。', 'SUCCEEDED', 'openai-compatible', 'content-reasoner', 'he_reader')
) as seed(task_type, target_type, article_slug, prompt, status, provider, model_name, username)
join articles article on article.slug = seed.article_slug
join users user_row on user_row.username = seed.username
where not exists (select 1 from ai_agent_tasks task where task.prompt = seed.prompt);

insert into ai_agent_messages (task_id, role, content, token_count)
select task.id, 'USER', task.prompt, 46 from ai_agent_tasks task where task.prompt like '请检查《%'
  and not exists (select 1 from ai_agent_messages message where message.task_id = task.id and message.role = 'USER');

insert into ai_suggestions (task_id, target_type, target_id, suggestion_type, content, status)
select task.id, task.target_type, task.target_id, 'DISPLAY_COPY', '建议把标题、摘要和标签理由写得更像真实读者会点击的入口。', 'PENDING' from ai_agent_tasks task where task.prompt like '请检查《%'
  and not exists (select 1 from ai_suggestions suggestion where suggestion.task_id = task.id and suggestion.suggestion_type = 'DISPLAY_COPY');

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
select
    'HOME',
    null,
    'reader.flow.reader-flow-01',
    'CUSTOM',
    '清晨阅读入口',
    '清晨打开首页时，读者更容易先点进短标题和明确标签，推荐入口需要保持安静但可见。',
    jsonb_build_object('scenario', 'reader-flow-01', 'surface', 'HOME', 'topic', '清晨阅读入口'),
    500,
    true
where not exists (
    select 1 from content_blocks where block_key = 'reader.flow.reader-flow-01'
);

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
select
    'ARTICLE',
    null,
    'reader.flow.reader-flow-02',
    'TEXT',
    '午休快速浏览',
    '午休时间浏览节奏很快，文章卡片需要让标题、摘要和标签在一屏内形成判断依据。',
    jsonb_build_object('scenario', 'reader-flow-02', 'surface', 'ARTICLE', 'topic', '午休快速浏览'),
    505,
    true
where not exists (
    select 1 from content_blocks where block_key = 'reader.flow.reader-flow-02'
);

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
select
    'PROJECT',
    null,
    'reader.flow.reader-flow-03',
    'MARKDOWN',
    '移动端筛选停留',
    '移动端筛选区需要让标签自然换行，避免控件拉长后遮挡后续内容。',
    jsonb_build_object('scenario', 'reader-flow-03', 'surface', 'PROJECT', 'topic', '移动端筛选停留'),
    510,
    true
where not exists (
    select 1 from content_blocks where block_key = 'reader.flow.reader-flow-03'
);

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
select
    'INSPIRATION',
    null,
    'reader.flow.reader-flow-04',
    'STAT',
    '后台审核回访',
    '管理员回访审核结果时，最需要看到状态、处理时间和相关内容的上下文。',
    jsonb_build_object('scenario', 'reader-flow-04', 'surface', 'INSPIRATION', 'topic', '后台审核回访'),
    515,
    true
where not exists (
    select 1 from content_blocks where block_key = 'reader.flow.reader-flow-04'
);

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
select
    'ABOUT',
    null,
    'reader.flow.reader-flow-05',
    'CTA',
    '作品档案二次打开',
    '二次打开作品档案时，截图、里程碑和资源链接要能快速唤起上次阅读的位置。',
    jsonb_build_object('scenario', 'reader-flow-05', 'surface', 'ABOUT', 'topic', '作品档案二次打开'),
    520,
    true
where not exists (
    select 1 from content_blocks where block_key = 'reader.flow.reader-flow-05'
);

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
select
    'HOME',
    null,
    'reader.flow.reader-flow-06',
    'CUSTOM',
    '灵感卡片回链',
    '灵感卡片如果能回链到文章和作品，读者就能理解它不是孤立片段。',
    jsonb_build_object('scenario', 'reader-flow-06', 'surface', 'HOME', 'topic', '灵感卡片回链'),
    525,
    true
where not exists (
    select 1 from content_blocks where block_key = 'reader.flow.reader-flow-06'
);

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
select
    'ARTICLE',
    null,
    'reader.flow.reader-flow-07',
    'TEXT',
    '搜索后继续阅读',
    '搜索后的继续阅读入口应该给出相邻主题，而不是只把读者送回列表。',
    jsonb_build_object('scenario', 'reader-flow-07', 'surface', 'ARTICLE', 'topic', '搜索后继续阅读'),
    530,
    true
where not exists (
    select 1 from content_blocks where block_key = 'reader.flow.reader-flow-07'
);

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
select
    'PROJECT',
    null,
    'reader.flow.reader-flow-08',
    'MARKDOWN',
    '标签别名命中',
    '标签别名命中后需要落到同一组内容，减少同义词带来的搜索分叉。',
    jsonb_build_object('scenario', 'reader-flow-08', 'surface', 'PROJECT', 'topic', '标签别名命中'),
    535,
    true
where not exists (
    select 1 from content_blocks where block_key = 'reader.flow.reader-flow-08'
);

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
select
    'INSPIRATION',
    null,
    'reader.flow.reader-flow-09',
    'STAT',
    '封面图查看',
    '封面图查看场景下，图片说明和内容标题要互相补充，避免只剩装饰感。',
    jsonb_build_object('scenario', 'reader-flow-09', 'surface', 'INSPIRATION', 'topic', '封面图查看'),
    540,
    true
where not exists (
    select 1 from content_blocks where block_key = 'reader.flow.reader-flow-09'
);

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
select
    'ABOUT',
    null,
    'reader.flow.reader-flow-10',
    'CTA',
    '留言板回访',
    '留言板回访时，读者更关心回复状态和上下文，排序不应只看发布时间。',
    jsonb_build_object('scenario', 'reader-flow-10', 'surface', 'ABOUT', 'topic', '留言板回访'),
    545,
    true
where not exists (
    select 1 from content_blocks where block_key = 'reader.flow.reader-flow-10'
);

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
select
    'HOME',
    null,
    'reader.flow.reader-flow-11',
    'CUSTOM',
    '主题切换预览',
    '主题切换预览要重点观察文字对比、按钮状态和卡片层级是否仍然稳定。',
    jsonb_build_object('scenario', 'reader-flow-11', 'surface', 'HOME', 'topic', '主题切换预览'),
    550,
    true
where not exists (
    select 1 from content_blocks where block_key = 'reader.flow.reader-flow-11'
);

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
select
    'ARTICLE',
    null,
    'reader.flow.reader-flow-12',
    'TEXT',
    '文章系列跳转',
    '文章系列跳转需要明确上一篇、下一篇和当前进度，降低读者迷路的概率。',
    jsonb_build_object('scenario', 'reader-flow-12', 'surface', 'ARTICLE', 'topic', '文章系列跳转'),
    555,
    true
where not exists (
    select 1 from content_blocks where block_key = 'reader.flow.reader-flow-12'
);

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
select
    'PROJECT',
    null,
    'reader.flow.reader-flow-13',
    'MARKDOWN',
    '收藏夹回看',
    '收藏夹回看场景下，内容摘要和最近互动能帮助读者决定是否继续阅读。',
    jsonb_build_object('scenario', 'reader-flow-13', 'surface', 'PROJECT', 'topic', '收藏夹回看'),
    560,
    true
where not exists (
    select 1 from content_blocks where block_key = 'reader.flow.reader-flow-13'
);

insert into content_blocks (owner_type, owner_id, block_key, block_type, title, body, config_json, sort_order, visible)
select
    'INSPIRATION',
    null,
    'reader.flow.reader-flow-14',
    'STAT',
    '评论提醒处理',
    '评论提醒处理要把原文、回复对象和审核状态放近一点，减少来回跳转。',
    jsonb_build_object('scenario', 'reader-flow-14', 'surface', 'INSPIRATION', 'topic', '评论提醒处理'),
    565,
    true
where not exists (
    select 1 from content_blocks where block_key = 'reader.flow.reader-flow-14'
);

insert into visit_logs (path, target_type, target_id, user_id, ip_address, user_agent, referer, device_type, browser, operating_system, created_at)
select
    '/articles/' || article.slug,
    'ARTICLE',
    article.id,
    user_row.id,
    '127.0.13.40'::inet,
    'CreatorSpace Reading Browser',
    '/search?keyword=清晨阅读入口',
    case when 0 % 3 = 0 then 'MOBILE' when 0 % 3 = 1 then 'DESKTOP' else 'TABLET' end,
    case when 0 % 2 = 0 then 'Chrome' else 'Edge' end,
    case when 0 % 3 = 0 then 'Android' when 0 % 3 = 1 then 'Windows' else 'iOS' end,
    now() - interval '6 hours'
from articles article
join users user_row on user_row.username = 'mira_frontend'
where article.slug = 'homepage-tag-recommendation'
  and not exists (
      select 1 from visit_logs log_row
      where log_row.user_id = user_row.id
        and log_row.target_type = 'ARTICLE'
        and log_row.target_id = article.id
        and log_row.referer = '/search?keyword=清晨阅读入口'
  );

insert into visit_logs (path, target_type, target_id, user_id, ip_address, user_agent, referer, device_type, browser, operating_system, created_at)
select
    '/articles/' || article.slug,
    'ARTICLE',
    article.id,
    user_row.id,
    '127.0.13.41'::inet,
    'CreatorSpace Reading Browser',
    '/search?keyword=午休快速浏览',
    case when 1 % 3 = 0 then 'MOBILE' when 1 % 3 = 1 then 'DESKTOP' else 'TABLET' end,
    case when 1 % 2 = 0 then 'Chrome' else 'Edge' end,
    case when 1 % 3 = 0 then 'Android' when 1 % 3 = 1 then 'Windows' else 'iOS' end,
    now() - interval '7 hours'
from articles article
join users user_row on user_row.username = 'zhou_backend'
where article.slug = 'unified-form-controls'
  and not exists (
      select 1 from visit_logs log_row
      where log_row.user_id = user_row.id
        and log_row.target_type = 'ARTICLE'
        and log_row.target_id = article.id
        and log_row.referer = '/search?keyword=午休快速浏览'
  );

insert into visit_logs (path, target_type, target_id, user_id, ip_address, user_agent, referer, device_type, browser, operating_system, created_at)
select
    '/articles/' || article.slug,
    'ARTICLE',
    article.id,
    user_row.id,
    '127.0.13.42'::inet,
    'CreatorSpace Reading Browser',
    '/search?keyword=移动端筛选停留',
    case when 2 % 3 = 0 then 'MOBILE' when 2 % 3 = 1 then 'DESKTOP' else 'TABLET' end,
    case when 2 % 2 = 0 then 'Chrome' else 'Edge' end,
    case when 2 % 3 = 0 then 'Android' when 2 % 3 = 1 then 'Windows' else 'iOS' end,
    now() - interval '8 hours'
from articles article
join users user_row on user_row.username = 'lin_visual'
where article.slug = 'security-public-recommendation-api'
  and not exists (
      select 1 from visit_logs log_row
      where log_row.user_id = user_row.id
        and log_row.target_type = 'ARTICLE'
        and log_row.target_id = article.id
        and log_row.referer = '/search?keyword=移动端筛选停留'
  );

insert into visit_logs (path, target_type, target_id, user_id, ip_address, user_agent, referer, device_type, browser, operating_system, created_at)
select
    '/articles/' || article.slug,
    'ARTICLE',
    article.id,
    user_row.id,
    '127.0.13.43'::inet,
    'CreatorSpace Reading Browser',
    '/search?keyword=后台审核回访',
    case when 3 % 3 = 0 then 'MOBILE' when 3 % 3 = 1 then 'DESKTOP' else 'TABLET' end,
    case when 3 % 2 = 0 then 'Chrome' else 'Edge' end,
    case when 3 % 3 = 0 then 'Android' when 3 % 3 = 1 then 'Windows' else 'iOS' end,
    now() - interval '9 hours'
from articles article
join users user_row on user_row.username = 'qiao_writer'
where article.slug = 'content-governance-field-notes'
  and not exists (
      select 1 from visit_logs log_row
      where log_row.user_id = user_row.id
        and log_row.target_type = 'ARTICLE'
        and log_row.target_id = article.id
        and log_row.referer = '/search?keyword=后台审核回访'
  );

insert into visit_logs (path, target_type, target_id, user_id, ip_address, user_agent, referer, device_type, browser, operating_system, created_at)
select
    '/articles/' || article.slug,
    'ARTICLE',
    article.id,
    user_row.id,
    '127.0.13.44'::inet,
    'CreatorSpace Reading Browser',
    '/search?keyword=作品档案二次打开',
    case when 4 % 3 = 0 then 'MOBILE' when 4 % 3 = 1 then 'DESKTOP' else 'TABLET' end,
    case when 4 % 2 = 0 then 'Chrome' else 'Edge' end,
    case when 4 % 3 = 0 then 'Android' when 4 % 3 = 1 then 'Windows' else 'iOS' end,
    now() - interval '10 hours'
from articles article
join users user_row on user_row.username = 'chen_product'
where article.slug = 'theme-blog-reading-path-design'
  and not exists (
      select 1 from visit_logs log_row
      where log_row.user_id = user_row.id
        and log_row.target_type = 'ARTICLE'
        and log_row.target_id = article.id
        and log_row.referer = '/search?keyword=作品档案二次打开'
  );

insert into visit_logs (path, target_type, target_id, user_id, ip_address, user_agent, referer, device_type, browser, operating_system, created_at)
select
    '/articles/' || article.slug,
    'ARTICLE',
    article.id,
    user_row.id,
    '127.0.13.45'::inet,
    'CreatorSpace Reading Browser',
    '/search?keyword=灵感卡片回链',
    case when 5 % 3 = 0 then 'MOBILE' when 5 % 3 = 1 then 'DESKTOP' else 'TABLET' end,
    case when 5 % 2 = 0 then 'Chrome' else 'Edge' end,
    case when 5 % 3 = 0 then 'Android' when 5 % 3 = 1 then 'Windows' else 'iOS' end,
    now() - interval '11 hours'
from articles article
join users user_row on user_row.username = 'he_reader'
where article.slug = 'inspiration-wall-relations'
  and not exists (
      select 1 from visit_logs log_row
      where log_row.user_id = user_row.id
        and log_row.target_type = 'ARTICLE'
        and log_row.target_id = article.id
        and log_row.referer = '/search?keyword=灵感卡片回链'
  );

insert into visit_logs (path, target_type, target_id, user_id, ip_address, user_agent, referer, device_type, browser, operating_system, created_at)
select
    '/articles/' || article.slug,
    'ARTICLE',
    article.id,
    user_row.id,
    '127.0.13.46'::inet,
    'CreatorSpace Reading Browser',
    '/search?keyword=搜索后继续阅读',
    case when 6 % 3 = 0 then 'MOBILE' when 6 % 3 = 1 then 'DESKTOP' else 'TABLET' end,
    case when 6 % 2 = 0 then 'Chrome' else 'Edge' end,
    case when 6 % 3 = 0 then 'Android' when 6 % 3 = 1 then 'Windows' else 'iOS' end,
    now() - interval '12 hours'
from articles article
join users user_row on user_row.username = 'mira_frontend'
where article.slug = 'admin-workbench-information-density'
  and not exists (
      select 1 from visit_logs log_row
      where log_row.user_id = user_row.id
        and log_row.target_type = 'ARTICLE'
        and log_row.target_id = article.id
        and log_row.referer = '/search?keyword=搜索后继续阅读'
  );

insert into visit_logs (path, target_type, target_id, user_id, ip_address, user_agent, referer, device_type, browser, operating_system, created_at)
select
    '/articles/' || article.slug,
    'ARTICLE',
    article.id,
    user_row.id,
    '127.0.13.47'::inet,
    'CreatorSpace Reading Browser',
    '/search?keyword=标签别名命中',
    case when 7 % 3 = 0 then 'MOBILE' when 7 % 3 = 1 then 'DESKTOP' else 'TABLET' end,
    case when 7 % 2 = 0 then 'Chrome' else 'Edge' end,
    case when 7 % 3 = 0 then 'Android' when 7 % 3 = 1 then 'Windows' else 'iOS' end,
    now() - interval '13 hours'
from articles article
join users user_row on user_row.username = 'zhou_backend'
where article.slug = 'project-archive-visual-story'
  and not exists (
      select 1 from visit_logs log_row
      where log_row.user_id = user_row.id
        and log_row.target_type = 'ARTICLE'
        and log_row.target_id = article.id
        and log_row.referer = '/search?keyword=标签别名命中'
  );

insert into visit_logs (path, target_type, target_id, user_id, ip_address, user_agent, referer, device_type, browser, operating_system, created_at)
select
    '/articles/' || article.slug,
    'ARTICLE',
    article.id,
    user_row.id,
    '127.0.13.48'::inet,
    'CreatorSpace Reading Browser',
    '/search?keyword=封面图查看',
    case when 8 % 3 = 0 then 'MOBILE' when 8 % 3 = 1 then 'DESKTOP' else 'TABLET' end,
    case when 8 % 2 = 0 then 'Chrome' else 'Edge' end,
    case when 8 % 3 = 0 then 'Android' when 8 % 3 = 1 then 'Windows' else 'iOS' end,
    now() - interval '14 hours'
from articles article
join users user_row on user_row.username = 'lin_visual'
where article.slug = 'homepage-tag-recommendation'
  and not exists (
      select 1 from visit_logs log_row
      where log_row.user_id = user_row.id
        and log_row.target_type = 'ARTICLE'
        and log_row.target_id = article.id
        and log_row.referer = '/search?keyword=封面图查看'
  );

