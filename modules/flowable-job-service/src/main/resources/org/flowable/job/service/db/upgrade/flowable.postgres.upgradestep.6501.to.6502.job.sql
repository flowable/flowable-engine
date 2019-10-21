create index ACT_IDX_JOB_TENANT_ID on ACT_RU_JOB(TENANT_ID_);

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.2' where NAME_ = 'job.schema.version';
