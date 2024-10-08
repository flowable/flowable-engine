alter table ACT_RU_EVENT_SUBSCR add SCOPE_DEFINITION_KEY_ nvarchar(255);

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'common.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'entitylink.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'identitylink.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'job.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'batch.schema.version';

alter table ACT_RU_TASK add STATE_ nvarchar(255);
alter table ACT_RU_TASK add IN_PROGRESS_TIME_ datetime;
alter table ACT_RU_TASK add IN_PROGRESS_STARTED_BY_ nvarchar(255);
alter table ACT_RU_TASK add CLAIMED_BY_ nvarchar(255);
alter table ACT_RU_TASK add SUSPENDED_TIME_ datetime;
alter table ACT_RU_TASK add SUSPENDED_BY_ nvarchar(255);
alter table ACT_RU_TASK add IN_PROGRESS_DUE_DATE_ datetime;

alter table ACT_HI_TASKINST add STATE_ nvarchar(255);
alter table ACT_HI_TASKINST add IN_PROGRESS_TIME_ datetime;
alter table ACT_HI_TASKINST add IN_PROGRESS_STARTED_BY_ nvarchar(255);
alter table ACT_HI_TASKINST add CLAIMED_BY_ nvarchar(255);
alter table ACT_HI_TASKINST add SUSPENDED_TIME_ datetime;
alter table ACT_HI_TASKINST add SUSPENDED_BY_ nvarchar(255);
alter table ACT_HI_TASKINST add COMPLETED_BY_ nvarchar(255);
alter table ACT_HI_TASKINST add IN_PROGRESS_DUE_DATE_ datetime;

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'task.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'variable.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'schema.version';

update ACT_ID_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'schema.version';
