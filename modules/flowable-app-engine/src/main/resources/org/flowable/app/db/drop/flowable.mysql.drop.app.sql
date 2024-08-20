drop index ACT_IDX_APP_RSRC_DPL on ACT_APP_DEPLOYMENT_RESOURCE;
drop index ACT_IDX_APP_DEF_DPLY on ACT_APP_APPDEF;
drop index ACT_IDX_APP_DEF_UNIQ on ACT_APP_APPDEF;

alter table ACT_APP_APPDEF
    drop foreign key ACT_FK_APP_DEF_DPLY;

drop table if exists ACT_APP_APPDEF;
drop table if exists ACT_APP_DEPLOYMENT_RESOURCE;
drop table if exists ACT_APP_DEPLOYMENT;
