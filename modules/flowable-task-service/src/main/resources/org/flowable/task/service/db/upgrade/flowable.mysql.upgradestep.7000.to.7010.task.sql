alter table ACT_RU_TASK add column (
    STATE_ varchar(255), 
    IN_PROGRESS_TIME_ datetime(3), 
    IN_PROGRESS_STARTED_BY_ varchar(255),
    CLAIMED_BY_ varchar(255), 
    SUSPENDED_TIME_ datetime(3), 
    SUSPENDED_BY_ varchar(255), 
    IN_PROGRESS_DUE_DATE_ datetime(3));

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.0' where NAME_ = 'task.schema.version';
