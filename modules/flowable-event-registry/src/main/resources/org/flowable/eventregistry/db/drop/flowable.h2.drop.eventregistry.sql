drop index if exists ACT_IDX_CHANNEL_DEF_UNIQ;
drop index if exists ACT_IDX_EVENT_DEF_UNIQ;

drop table if exists FLW_CHANNEL_DEFINITION cascade constraints;
drop table if exists FLW_EVENT_DEFINITION cascade constraints;
drop table if exists FLW_EVENT_RESOURCE cascade constraints;
drop table if exists FLW_EVENT_DEPLOYMENT cascade constraints;
