drop table if exists ACT_RE_DEPLOYMENT cascade constraints;
drop table if exists ACT_RE_MODEL cascade constraints;
drop table if exists ACT_RU_EXECUTION cascade constraints;
drop table if exists ACT_RE_PROCDEF cascade constraints;
drop table if exists ACT_RU_EVENT_SUBSCR cascade constraints;
drop table if exists ACT_EVT_LOG cascade constraints;
drop table if exists ACT_PROCDEF_INFO cascade constraints;

drop index if exists ACT_IDX_EXEC_BUSKEY;
drop index if exists ACT_IDX_VARIABLE_TASK_ID;
drop index if exists ACT_IDX_EVENT_SUBSCR_CONFIG_;
drop index if exists ACT_IDX_ATHRZ_PROCEDEF;
drop index if exists ACT_IDX_INFO_PROCDEF;
drop index if exists ACT_IDX_JOB_SCOPE;
drop index if exists ACT_IDX_JOB_SUB_SCOPE;
drop index if exists ACT_IDX_JOB_SCOPE_DEF;
drop index if exists ACT_IDX_TJOB_SCOPE;
drop index if exists ACT_IDX_TJOB_SUB_SCOPE;
drop index if exists ACT_IDX_TJOB_SCOPE_DEF;
drop index if exists ACT_IDX_SJOB_SCOPE;
drop index if exists ACT_IDX_SJOB_SUB_SCOPE;
drop index if exists ACT_IDX_SJOB_SCOPE_DEF;
drop index if exists ACT_IDX_DJOB_SCOPE;
drop index if exists ACT_IDX_DJOB_SUB_SCOPE;
drop index if exists ACT_IDX_DJOB_SCOPE_DEF;  

drop table if exists ACT_RU_JOB cascade constraints;
drop table if exists ACT_RU_TIMER_JOB cascade constraints;
drop table if exists ACT_RU_SUSPENDED_JOB cascade constraints;
drop table if exists ACT_RU_DEADLETTER_JOB cascade constraints;
drop table if exists ACT_RU_HISTORY_JOB cascade constraints;
drop table if exists ACT_RU_VARIABLE cascade constraints;

drop table if exists ACT_HI_TASKINST cascade constraints;
drop table if exists ACT_RU_IDENTITYLINK cascade constraints;

drop index if exists ACT_IDX_IDENT_LNK_USER;
drop index if exists ACT_IDX_IDENT_LNK_GROUP;
drop index if exists ACT_IDX_IDENT_LNK_SCOPE;
drop index if exists ACT_IDX_IDENT_LNK_SCOPE_DEF;
drop table if exists ACT_GE_BYTEARRAY cascade constraints;
drop table if exists ACT_GE_PROPERTY cascade constraints;
