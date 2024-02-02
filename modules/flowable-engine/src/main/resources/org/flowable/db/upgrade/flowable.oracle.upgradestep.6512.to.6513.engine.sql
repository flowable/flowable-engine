alter table ACT_RU_EXECUTION add LOCK_OWNER_ NVARCHAR2(255);
alter table ACT_RU_EXECUTION add EXTERNAL_WORKER_JOB_COUNT_ INTEGER;

update ACT_GE_PROPERTY set VALUE_ = '6.5.1.3' where NAME_ = 'schema.version';
