alter table ACT_RU_EXECUTION add column LOCK_OWNER_ varchar(255);
alter table ACT_RU_EXECUTION add column EXTERNAL_WORKER_JOB_COUNT_ integer;

alter table ACT_RU_ACTINST add column TRANSACTION_ORDER_ integer;

alter table ACT_HI_ACTINST add column TRANSACTION_ORDER_ integer;

update ACT_GE_PROPERTY set VALUE_ = '6.6.0.0' where NAME_ = 'schema.version';
