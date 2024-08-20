drop table if exists ACT_APP_APPDEF cascade constraints;
drop table if exists ACT_APP_DEPLOYMENT_RESOURCE cascade constraints;
drop table if exists ACT_APP_DEPLOYMENT cascade constraints;

drop index if exists ACT_IDX_APP_RSRC_DPL;
drop index if exists ACT_IDX_APP_DEF_DPLY;
drop index if exists ACT_IDX_APP_DEF_UNIQ;
