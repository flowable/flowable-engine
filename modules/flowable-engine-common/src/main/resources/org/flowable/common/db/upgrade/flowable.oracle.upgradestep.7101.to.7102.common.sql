create index ACT_IDX_EVENT_SUBSCR_PROC_ID on ACT_RU_EVENT_SUBSCR(PROC_INST_ID_);

update ACT_GE_PROPERTY set VALUE_ = '7.1.0.2' where NAME_ = 'common.schema.version';
