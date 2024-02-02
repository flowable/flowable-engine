alter table ACT_HI_TASKINST add (
    STATE_ varchar(255), 
    IN_PROGRESS_TIME_ timestamp, 
    IN_PROGRESS_STARTED_BY_ varchar(255),
    CLAIMED_BY_ varchar(255), 
    SUSPENDED_TIME_ timestamp, 
    SUSPENDED_BY_ varchar(255), 
    COMPLETED_BY_ varchar(255), 
    IN_PROGRESS_DUE_DATE_ timestamp);