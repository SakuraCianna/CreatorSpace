insert into roles (code, name, description)
values
    ('ADMIN', '管理员', '拥有后台 CMS 和站点配置管理权限'),
    ('USER', '注册用户', '可登录、评论、点赞和收藏公开内容')
on conflict (code) do nothing;

do $$
begin
    if '${adminPasswordHash}' !~ '^\$2[aby]\$[0-9]{2}\$.{53}$' then
        raise exception 'ADMIN_PASSWORD_HASH must be a BCrypt hash';
    end if;
end $$;

insert into users (username, nickname, password_hash, status)
values ('${adminUsername}', '管理员', '${adminPasswordHash}', 'ACTIVE')
on conflict (username) do nothing;

insert into user_roles (user_id, role_id)
select u.id, r.id
from users u
join roles r on r.code = 'ADMIN'
where u.username = '${adminUsername}'
on conflict (user_id, role_id) do nothing;

insert into theme_configs (
    theme_name,
    display_name,
    primary_color,
    background_type,
    font_family,
    card_style,
    layout_type,
    is_active,
    config_json
)
values (
    'glass-space',
    'Glass Space',
    '#7c3aed',
    'star',
    'Inter, system-ui, sans-serif',
    'glass',
    'bento',
    true,
    '{"background": "star", "cardStyle": "glass", "layout": "bento"}'::jsonb
)
on conflict (theme_name) do nothing;
