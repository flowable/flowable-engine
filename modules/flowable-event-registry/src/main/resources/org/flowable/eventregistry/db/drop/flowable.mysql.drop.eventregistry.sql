alter table FLW_EVENT_RESOURCE drop foreign key FLW_FK_EVENT_RSRC_DPL;

drop index FLW_IDX_EVENT_RSRC_DPL on FLW_EVENT_RESOURCE;
drop index ACT_IDX_CHANNEL_DEF_UNIQ on FLW_CHANNEL_DEFINITION;
drop index ACT_IDX_EVENT_DEF_UNIQ on FLW_EVENT_DEFINITION;

drop table if exists FLW_CHANNEL_DEFINITION;
drop table if exists FLW_EVENT_DEFINITION;
drop table if exists FLW_EVENT_RESOURCE;
drop table if exists FLW_EVENT_DEPLOYMENT;
