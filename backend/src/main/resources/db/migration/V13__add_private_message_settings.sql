-- 添加用户私信权限控制设置
alter table users add column private_message_setting varchar(20) not null default 'ALL';
comment on column users.private_message_setting is '私信设置: ALL-所有人, FOLLOW-关注我才可私信, MUTUAL-仅互相关注, NONE-关闭私信';
