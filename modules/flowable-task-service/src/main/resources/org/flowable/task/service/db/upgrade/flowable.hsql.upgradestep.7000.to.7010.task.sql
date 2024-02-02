alter table ACT_RU_TASK add (
    STATE_ varchar(255), 
    IN_PROGRESS_TIME_ timestamp, 
    IN_PROGRESS_STARTED_BY_ varchar(255),
    CLAIMED_BY_ varchar(255), 
    SUSPENDED_TIME_ timestamp, 
    SUSPENDED_BY_ varchar(255), 
    IN_PROGRESS_DUE_DATE_ timestamp);

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.0' where NAME_ = 'task.schema.version';
