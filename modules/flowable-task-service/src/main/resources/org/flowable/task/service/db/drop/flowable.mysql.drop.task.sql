drop index ACT_IDX_TASK_CREATE on ACT_RU_TASK;
drop index ACT_IDX_TASK_SCOPE on ACT_RU_TASK;
drop index ACT_IDX_TASK_SUB_SCOPE on ACT_RU_TASK;
drop index ACT_IDX_TASK_SCOPE_DEF on ACT_RU_TASK;
drop index FLW_IDX_TASK_LOG_NUMBER on FLW_TSK_LOG;

drop table if exists ACT_RU_TASK;
drop table if exists FLW_TSK_LOG;