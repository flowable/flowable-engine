drop table if exists ACT_RU_ACTINST cascade constraints;
drop table if exists ACT_RE_DEPLOYMENT cascade constraints;
drop table if exists ACT_RE_MODEL cascade constraints;
drop table if exists ACT_RU_EXECUTION cascade constraints;
drop table if exists ACT_RE_PROCDEF cascade constraints;
drop table if exists ACT_EVT_LOG cascade constraints;
drop table if exists ACT_PROCDEF_INFO cascade constraints;

drop index if exists ACT_IDX_EXEC_BUSKEY;
drop index if exists ACT_IDX_VARIABLE_TASK_ID;
drop index if exists ACT_IDX_ATHRZ_PROCEDEF;
drop index if exists ACT_IDX_INFO_PROCDEF;

drop index if exists ACT_IDX_RU_ACTI_START;
drop index if exists ACT_IDX_RU_ACTI_END;
drop index if exists ACT_IDX_RU_ACTI_PROC;
drop index if exists ACT_IDX_RU_ACTI_PROC_ACT;
drop index if exists ACT_IDX_RU_ACTI_EXEC;
drop index if exists ACT_IDX_RU_ACTI_EXEC_ACT;
