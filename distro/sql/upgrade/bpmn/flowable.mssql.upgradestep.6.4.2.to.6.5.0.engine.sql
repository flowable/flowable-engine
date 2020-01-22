alter table ACT_RU_EXECUTION add REFERENCE_ID_ nvarchar(255);
alter table ACT_RU_EXECUTION add REFERENCE_TYPE_ nvarchar(255);

alter table ACT_RU_EXECUTION add PROPAGATED_STAGE_INST_ID_ nvarchar(255);

alter table ACT_HI_PROCINST add REFERENCE_ID_ nvarchar(255);
alter table ACT_HI_PROCINST add REFERENCE_TYPE_ nvarchar(255);

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.6' where NAME_ = 'schema.version';
