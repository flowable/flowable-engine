update ACT_GE_PROPERTY set VALUE_ = '7.0.0.0' where NAME_ = 'common.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.0.0' where NAME_ = 'entitylink.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.0.0' where NAME_ = 'identitylink.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.0.0' where NAME_ = 'job.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.0.0' where NAME_ = 'batch.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.0.0' where NAME_ = 'task.schema.version';

alter table ACT_RU_VARIABLE add META_INFO_ nvarchar(4000);

alter table ACT_HI_VARINST add META_INFO_ nvarchar(4000);

update ACT_GE_PROPERTY set VALUE_ = '7.0.0.0' where NAME_ = 'variable.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.0.0' where NAME_ = 'schema.version';

update ACT_ID_PROPERTY set VALUE_ = '7.0.0.0' where NAME_ = 'schema.version';
