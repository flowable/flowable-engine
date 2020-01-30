alter table ACT_RU_EXECUTION add REFERENCE_ID_ NVARCHAR2(255);
alter table ACT_RU_EXECUTION add REFERENCE_TYPE_ NVARCHAR2(255);

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.4' where NAME_ = 'schema.version';