update ACT_GE_PROPERTY set VALUE_ = '6.1.1.0' where NAME_ = 'schema.version';

alter table ACT_RU_JOB add column CREATE_TIME_ timestamp;
alter table ACT_RU_TIMER_JOB add column CREATE_TIME_ timestamp;
alter table ACT_RU_SUSPENDED_JOB add column CREATE_TIME_ timestamp;
alter table ACT_RU_DEADLETTER_JOB add column CREATE_TIME_ timestamp;

update ACT_RU_JOB set CREATE_TIME_=CURRENT_TIMESTAMP;
update ACT_RU_TIMER_JOB set CREATE_TIME_=CURRENT_TIMESTAMP;
update ACT_RU_SUSPENDED_JOB set CREATE_TIME_=CURRENT_TIMESTAMP;
update ACT_RU_DEADLETTER_JOB set CREATE_TIME_=CURRENT_TIMESTAMP;
