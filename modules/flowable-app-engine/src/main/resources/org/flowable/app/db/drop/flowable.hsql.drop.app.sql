drop table if exists ACT_APP_APPDEF cascade;
drop table if exists ACT_APP_DEPLOYMENT_RESOURCE cascade;
drop table if exists ACT_APP_DEPLOYMENT cascade;

drop index if exists ACT_IDX_APP_RSRC_DPL;
drop index if exists ACT_IDX_APP_DEF_DPLY;
drop index if exists ACT_IDX_APP_DEF_UNIQ;
