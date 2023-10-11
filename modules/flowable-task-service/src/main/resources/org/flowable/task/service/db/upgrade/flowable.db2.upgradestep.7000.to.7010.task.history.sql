alter table ACT_HI_TASKINST 
  add column STATE_ varchar(255)
  add column IN_PROGRESS_TIME_ timestamp
  add column IN_PROGRESS_STARTED_BY_ varchar(255)
  add column CLAIMED_BY_ varchar(255)
  add column SUSPENDED_TIME_ timestamp
  add column SUSPENDED_BY_ varchar(255)
  add column COMPLETED_BY_ varchar(255)
  add column IN_PROGRESS_DUE_DATE_ timestamp;
