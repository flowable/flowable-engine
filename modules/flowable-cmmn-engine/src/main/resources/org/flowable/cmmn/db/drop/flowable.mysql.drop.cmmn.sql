alter table ACT_CMMN_RU_MIL_INST drop foreign key ACT_FK_MIL_CASE_INST;
alter table ACT_CMMN_RU_MIL_INST drop foreign key ACT_FK_MIL_CASE_DEF;
alter table ACT_CMMN_RU_SENTRY_PART_INST drop foreign key ACT_FK_SENTRY_PLAN_ITEM;
alter table ACT_CMMN_RU_SENTRY_PART_INST drop foreign key ACT_FK_SENTRY_CASE_INST;
alter table ACT_CMMN_RU_SENTRY_PART_INST drop foreign key ACT_FK_SENTRY_CASE_DEF;
alter table ACT_CMMN_RU_PLAN_ITEM_INST drop foreign key ACT_FK_PLAN_ITEM_CASE_INST;
alter table ACT_CMMN_RU_PLAN_ITEM_INST drop foreign key ACT_FK_PLAN_ITEM_CASE_DEF;
alter table ACT_CMMN_RU_CASE_INST drop foreign key ACT_FK_CASE_INST_CASE_DEF;
alter table ACT_CMMN_CASEDEF drop foreign key ACT_FK_CASE_DEF_DPLY;
alter table ACT_CMMN_DEPLOYMENT_RESOURCE drop foreign key ACT_FK_CMMN_RSRC_DPL;

drop index ACT_IDX_HI_PLAN_ITEM_INST_CASE on ACT_CMMN_HI_PLAN_ITEM_INST;
drop index ACT_IDX_HI_PLAN_ITEM_INST_CASE on ACT_CMMN_HI_PLAN_ITEM_INST;
drop index ACT_IDX_CASE_INST_REF_ID_ on ACT_CMMN_RU_CASE_INST;
drop index ACT_IDX_CASE_DEF_UNIQ on ACT_CMMN_CASEDEF;
drop index ACT_IDX_PLAN_ITEM_STAGE_INST on ACT_CMMN_RU_PLAN_ITEM_INST;
drop index ACT_IDX_MIL_CASE_INST on ACT_CMMN_RU_MIL_INST;
drop index ACT_IDX_MIL_CASE_DEF on ACT_CMMN_RU_MIL_INST;
drop index ACT_IDX_SENTRY_PLAN_ITEM on ACT_CMMN_RU_SENTRY_PART_INST;
drop index ACT_IDX_SENTRY_CASE_INST on ACT_CMMN_RU_SENTRY_PART_INST;
drop index ACT_IDX_SENTRY_CASE_DEF on ACT_CMMN_RU_SENTRY_PART_INST;
drop index ACT_IDX_PLAN_ITEM_CASE_INST on ACT_CMMN_RU_PLAN_ITEM_INST;
drop index ACT_IDX_PLAN_ITEM_CASE_DEF on ACT_CMMN_RU_PLAN_ITEM_INST;
drop index ACT_IDX_CASE_INST_PARENT on ACT_CMMN_RU_CASE_INST;
drop index ACT_IDX_CASE_INST_CASE_DEF on ACT_CMMN_RU_CASE_INST;
drop index ACT_IDX_CASE_DEF_DPLY on ACT_CMMN_CASEDEF;
drop index ACT_IDX_CMMN_RSRC_DPL on ACT_CMMN_DEPLOYMENT_RESOURCE;

drop table if exists ACT_CMMN_HI_PLAN_ITEM_INST;
drop table if exists ACT_CMMN_HI_MIL_INST;
drop table if exists ACT_CMMN_HI_CASE_INST;
drop table if exists ACT_CMMN_RU_MIL_INST;
drop table if exists ACT_CMMN_RU_SENTRY_PART_INST;
drop table if exists ACT_CMMN_RU_PLAN_ITEM_INST;
drop table if exists ACT_CMMN_RU_CASE_INST;
drop table if exists ACT_CMMN_CASEDEF;
drop table if exists ACT_CMMN_DEPLOYMENT_RESOURCE;
drop table if exists ACT_CMMN_DEPLOYMENT;
