drop table if exists ACT_HI_TASKINST cascade constraints;
drop table if exists ACT_HI_TSK_LOG cascade constraints;

drop index if exists ACT_IDX_HI_TASK_SCOPE;
drop index if exists ACT_IDX_HI_TASK_SUB_SCOPE;
drop index if exists ACT_IDX_HI_TASK_SCOPE_DEF;