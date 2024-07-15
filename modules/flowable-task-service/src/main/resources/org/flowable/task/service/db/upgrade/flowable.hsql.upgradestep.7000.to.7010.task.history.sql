alter table ACT_HI_TASKINST add column STATE_ varchar(255);
alter table ACT_HI_TASKINST add column IN_PROGRESS_TIME_ timestamp;
alter table ACT_HI_TASKINST add column IN_PROGRESS_STARTED_BY_ varchar(255);
alter table ACT_HI_TASKINST add column CLAIMED_BY_ varchar(255);
alter table ACT_HI_TASKINST add column SUSPENDED_TIME_ timestamp;
alter table ACT_HI_TASKINST add column SUSPENDED_BY_ varchar(255);
alter table ACT_HI_TASKINST add column COMPLETED_BY_ varchar(255);
alter table ACT_HI_TASKINST add column IN_PROGRESS_DUE_DATE_ timestamp;