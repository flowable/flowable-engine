alter table ACT_HI_TASKINST add (
    STATE_ NVARCHAR2(255), 
    IN_PROGRESS_TIME_ TIMESTAMP(6), 
    IN_PROGRESS_STARTED_BY_ NVARCHAR2(255),
    CLAIMED_BY_ NVARCHAR2(255), 
    SUSPENDED_TIME_ TIMESTAMP(6), 
    SUSPENDED_BY_ NVARCHAR2(255), 
    COMPLETED_BY_ NVARCHAR2(255), 
    IN_PROGRESS_DUE_DATE_ TIMESTAMP(6));