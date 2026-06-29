alter table like_records
    drop constraint if exists ck_like_records_target_type;

alter table like_records
    add constraint ck_like_records_target_type
        check (target_type in ('ARTICLE', 'PROJECT', 'COMMENT', 'INSPIRATION', 'MESSAGE'));
