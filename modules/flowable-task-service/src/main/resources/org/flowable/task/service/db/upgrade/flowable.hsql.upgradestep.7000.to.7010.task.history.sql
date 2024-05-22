alter table add column ACT_HI_TASKINST STATE_ varchar(255);
alter table add column ACT_HI_TASKINST IN_PROGRESS_TIME_ timestamp;
alter table add column ACT_HI_TASKINST IN_PROGRESS_STARTED_BY_ varchar(255);
alter table add column ACT_HI_TASKINST CLAIMED_BY_ varchar(255);
alter table add column ACT_HI_TASKINST SUSPENDED_TIME_ timestamp;
alter table add column ACT_HI_TASKINST SUSPENDED_BY_ varchar(255);
alter table add column ACT_HI_TASKINST COMPLETED_BY_ varchar(255);
alter table add column ACT_HI_TASKINST IN_PROGRESS_DUE_DATE_ timestamp;