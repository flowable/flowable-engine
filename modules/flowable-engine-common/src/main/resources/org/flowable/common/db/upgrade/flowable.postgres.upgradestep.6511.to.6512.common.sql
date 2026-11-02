update ACT_GE_PROPERTY set VALUE_ = '6.5.1.2' where NAME_ = 'common.schema.version';


alter table ACT_RU_JOB add column CATEGORY_ varchar(255);
alter table ACT_RU_TIMER_JOB add column CATEGORY_ varchar(255);
alter table ACT_RU_SUSPENDED_JOB add column CATEGORY_ varchar(255);
alter table ACT_RU_DEADLETTER_JOB add column CATEGORY_ varchar(255);

