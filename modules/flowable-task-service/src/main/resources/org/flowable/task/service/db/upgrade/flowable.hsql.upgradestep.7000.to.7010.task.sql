alter table ACT_RU_TASK add column STATE_ varchar(255);
alter table ACT_RU_TASK add column IN_PROGRESS_TIME_ timestamp;
alter table ACT_RU_TASK add column IN_PROGRESS_STARTED_BY_ varchar(255);
alter table ACT_RU_TASK add column CLAIMED_BY_ varchar(255);
alter table ACT_RU_TASK add column SUSPENDED_TIME_ timestamp;
alter table ACT_RU_TASK add column SUSPENDED_BY_ varchar(255);
alter table ACT_RU_TASK add column IN_PROGRESS_DUE_DATE_ timestamp;

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.0' where NAME_ = 'task.schema.version';
