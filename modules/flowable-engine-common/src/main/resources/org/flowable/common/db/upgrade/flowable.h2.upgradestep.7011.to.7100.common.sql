update ACT_GE_PROPERTY set VALUE_ = '7.1.0.0' where NAME_ = 'common.schema.version';



create index ACT_IDX_ACT_HI_TSK_LOG_TASK on ACT_HI_TSK_LOG(TASK_ID_);