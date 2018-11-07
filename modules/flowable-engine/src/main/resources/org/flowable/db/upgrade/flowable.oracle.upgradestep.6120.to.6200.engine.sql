alter table ACT_RU_EXECUTION add CALLBACK_ID_ NVARCHAR2(255);
alter table ACT_RU_EXECUTION add CALLBACK_TYPE_ NVARCHAR2(255);

update ACT_GE_PROPERTY set VALUE_ = '6.2.0.0' where NAME_ = 'schema.version';