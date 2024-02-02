alter table ACT_RU_JOB add CATEGORY_ NVARCHAR2(255);

alter table ACT_RU_TIMER_JOB add CATEGORY_ NVARCHAR2(255);

alter table ACT_RU_SUSPENDED_JOB add CATEGORY_ NVARCHAR2(255);

alter table ACT_RU_DEADLETTER_JOB add CATEGORY_ NVARCHAR2(255);

update ACT_GE_PROPERTY set VALUE_ = '6.5.1.2' where NAME_ = 'job.schema.version';
