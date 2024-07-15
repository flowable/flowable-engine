drop index ACT_IDX_APP_RSRC_DPL;
drop index ACT_IDX_APP_DEF_DPLY;
drop index ACT_IDX_APP_DEF_UNIQ;

alter table ACT_APP_APPDEF
    drop constraint ACT_FK_APP_DEF_DPLY;

drop table ACT_APP_APPDEF;
drop table ACT_APP_DEPLOYMENT_RESOURCE;
drop table ACT_APP_DEPLOYMENT;
