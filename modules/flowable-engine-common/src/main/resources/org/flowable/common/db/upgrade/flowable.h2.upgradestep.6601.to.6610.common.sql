update ACT_GE_PROPERTY set VALUE_ = '6.6.1.0' where NAME_ = 'common.schema.version';


create index ACT_IDX_TIMER_JOB_DUEDATE on ACT_RU_TIMER_JOB(DUEDATE_); 

