drop table if exists ACT_RU_TASK cascade;
drop table if exists FLW_HI_TSK_LOG cascade;

drop index if exists ACT_IDX_TASK_CREATE;
drop index if exists ACT_IDX_TASK_SCOPE;
drop index if exists ACT_IDX_TASK_SUB_SCOPE;
drop index if exists ACT_IDX_TASK_SCOPE_DEF;
drop index if exists FLW_IDX_HI_TASK_LOG_NUMBER;