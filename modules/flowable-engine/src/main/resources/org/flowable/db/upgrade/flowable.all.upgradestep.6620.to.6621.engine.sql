create index ACT_IDX_EXEC_REF_ID_ on ACT_RU_EXECUTION(REFERENCE_ID_);
create index ACT_IDX_RU_ACTI_TASK on ACT_RU_ACTINST(TASK_ID_);
update ACT_GE_PROPERTY set VALUE_ = '6.6.2.1' where NAME_ = 'schema.version';
