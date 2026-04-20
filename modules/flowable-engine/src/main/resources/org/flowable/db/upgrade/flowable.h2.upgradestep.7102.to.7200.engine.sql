alter table ACT_RU_ACTINST add column COMPLETED_BY_ varchar(255);

update ACT_GE_PROPERTY set VALUE_ = '7.2.0.0' where NAME_ = 'schema.version';