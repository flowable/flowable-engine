ALTER TABLE ACT_CMMN_HI_CASE_INST ADD END_USER_ID_ VARCHAR(255) NULL;

alter table ACT_CMMN_RU_PLAN_ITEM_INST add FAILED_TIME_ datetime(3) null;
alter table ACT_CMMN_HI_PLAN_ITEM_INST add FAILED_TIME_ datetime(3) null;

update ACT_GE_PROPERTY set VALUE_ = '8.0.0.0' where NAME_ = 'cmmn.schema.version';