alter table comments
    add column if not exists content_original text;

alter table guestbook_entries
    add column if not exists content_original text;

comment on column comments.content_original is '评论原始内容';
comment on column guestbook_entries.content_original is '留言原始内容';
