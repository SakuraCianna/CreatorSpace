create index if not exists idx_theme_versions_theme_version_no
    on theme_versions(theme_id, version_no desc);

insert into theme_versions (theme_id, version_no, snapshot_json, created_by)
select theme.id,
       coalesce((
           select max(existing.version_no) + 1
           from theme_versions existing
           where existing.theme_id = theme.id
       ), 1),
       jsonb_build_object(
           'source', 'MIGRATION_BASELINE',
           'themeName', theme.theme_name,
           'displayName', theme.display_name,
           'primaryColor', theme.primary_color,
           'backgroundType', theme.background_type,
           'backgroundImage', theme.background_image,
           'fontFamily', theme.font_family,
           'cardStyle', theme.card_style,
           'layoutType', theme.layout_type,
           'config', theme.config_json
       ),
       null
from theme_configs theme
where not exists (
    select 1
    from theme_versions complete_version
    where complete_version.theme_id = theme.id
      and complete_version.snapshot_json ? 'displayName'
      and complete_version.snapshot_json ? 'primaryColor'
      and complete_version.snapshot_json ? 'config'
);
