drop index ACT_IDX_HI_TASK_SCOPE on ACT_HI_TASKINST;
drop index ACT_IDX_HI_TASK_SUB_SCOPE on ACT_HI_TASKINST;
drop index ACT_IDX_HI_TASK_SCOPE_DEF on ACT_HI_TASKINST;

drop table if exists ACT_HI_TASKINST;
drop table if exists ACT_HI_TSK_LOG;