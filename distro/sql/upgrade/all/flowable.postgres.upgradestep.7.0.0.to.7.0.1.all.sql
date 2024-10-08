alter table ACT_RU_EVENT_SUBSCR add column SCOPE_DEFINITION_KEY_ varchar(255);

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'common.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'entitylink.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'identitylink.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'job.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'batch.schema.version';

alter table ACT_RU_TASK 
    add column STATE_ varchar(255), 
    add column IN_PROGRESS_TIME_ timestamp, 
    add column IN_PROGRESS_STARTED_BY_ varchar(255),
    add column CLAIMED_BY_ varchar(255), 
    add column SUSPENDED_TIME_ timestamp, 
    add column SUSPENDED_BY_ varchar(255), 
    add column IN_PROGRESS_DUE_DATE_ timestamp;

alter table ACT_HI_TASKINST 
    add column STATE_ varchar(255), 
    add column IN_PROGRESS_TIME_ timestamp, 
    add column IN_PROGRESS_STARTED_BY_ varchar(255),
    add column CLAIMED_BY_ varchar(255), 
    add column SUSPENDED_TIME_ timestamp, 
    add column SUSPENDED_BY_ varchar(255), 
    add column COMPLETED_BY_ varchar(255), 
    add column IN_PROGRESS_DUE_DATE_ timestamp;

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'task.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'variable.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'schema.version';

update ACT_ID_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'schema.version';
