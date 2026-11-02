alter table ACT_RU_EVENT_SUBSCR add SCOPE_DEFINITION_KEY_ NVARCHAR2(255);

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'common.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'entitylink.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'identitylink.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'job.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'batch.schema.version';

alter table ACT_RU_TASK add (
    STATE_ NVARCHAR2(255), 
    IN_PROGRESS_TIME_ TIMESTAMP(6), 
    IN_PROGRESS_STARTED_BY_ NVARCHAR2(255),
    CLAIMED_BY_ NVARCHAR2(255), 
    SUSPENDED_TIME_ TIMESTAMP(6), 
    SUSPENDED_BY_ NVARCHAR2(255), 
    IN_PROGRESS_DUE_DATE_ TIMESTAMP(6));

alter table ACT_HI_TASKINST add (
    STATE_ NVARCHAR2(255), 
    IN_PROGRESS_TIME_ TIMESTAMP(6), 
    IN_PROGRESS_STARTED_BY_ NVARCHAR2(255),
    CLAIMED_BY_ NVARCHAR2(255), 
    SUSPENDED_TIME_ TIMESTAMP(6), 
    SUSPENDED_BY_ NVARCHAR2(255), 
    COMPLETED_BY_ NVARCHAR2(255), 
    IN_PROGRESS_DUE_DATE_ TIMESTAMP(6));

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'task.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'variable.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'schema.version';

update ACT_ID_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'schema.version';
