alter table ACT_RU_EXECUTION add column REFERENCE_ID_ varchar(255);
alter table ACT_RU_EXECUTION add column REFERENCE_TYPE_ varchar(255);

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.4' where NAME_ = 'schema.version';