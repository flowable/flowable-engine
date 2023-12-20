alter table ACT_HI_TASKINST add STATE_ nvarchar(255);
alter table ACT_HI_TASKINST add IN_PROGRESS_TIME_ datetime;
alter table ACT_HI_TASKINST add IN_PROGRESS_STARTED_BY_ nvarchar(255);
alter table ACT_HI_TASKINST add CLAIMED_BY_ nvarchar(255);
alter table ACT_HI_TASKINST add SUSPENDED_TIME_ datetime;
alter table ACT_HI_TASKINST add SUSPENDED_BY_ nvarchar(255);
alter table ACT_HI_TASKINST add COMPLETED_BY_ nvarchar(255);
alter table ACT_HI_TASKINST add IN_PROGRESS_DUE_DATE_ datetime;