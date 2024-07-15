drop table if exists FLW_RU_BATCH_PART cascade constraints;
drop table if exists FLW_RU_BATCH cascade constraints;

drop index if exists FLW_IDX_BATCH_PART;

drop table if exists ACT_RU_ENTITYLINK cascade constraints;

drop index if exists ACT_IDX_ENT_LNK_SCOPE;
drop index if exists ACT_IDX_ENT_LNK_SCOPE_DEF;

drop table if exists ACT_HI_ENTITYLINK cascade constraints;

drop index if exists ACT_IDX_HI_ENT_LNK_SCOPE;
drop index if exists ACT_IDX_HI_ENT_LNK_SCOPE_DEF;

drop table if exists ACT_RU_EVENT_SUBSCR cascade constraints;

drop index if exists ACT_IDX_EVENT_SUBSCR_CONFIG_;
drop index if exists ACT_IDX_EVENT_SUBSCR_SCOPEREF_;

drop table if exists ACT_RU_IDENTITYLINK cascade constraints;

drop index if exists ACT_IDX_IDENT_LNK_USER;
drop index if exists ACT_IDX_IDENT_LNK_GROUP;
drop index if exists ACT_IDX_IDENT_LNK_SCOPE;
drop index if exists ACT_IDX_IDENT_LNK_SUB_SCOPE;
drop index if exists ACT_IDX_IDENT_LNK_SCOPE_DEF;

drop table if exists ACT_HI_IDENTITYLINK cascade constraints;

drop index if exists ACT_IDX_HI_IDENT_LNK_USER;
drop index if exists ACT_IDX_HI_IDENT_LNK_SCOPE;
drop index if exists ACT_IDX_HI_IDENT_LNK_SUB_SCOPE;
drop index if exists ACT_IDX_HI_IDENT_LNK_SCOPE_DEF;

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
drop index if exists ACT_IDX_EJOB_SCOPE;
drop index if exists ACT_IDX_EJOB_SUB_SCOPE;
drop index if exists ACT_IDX_EJOB_SCOPE_DEF;

drop table if exists ACT_RU_JOB cascade constraints;
drop table if exists ACT_RU_EXTERNAL_JOB cascade constraints;
drop table if exists ACT_RU_TIMER_JOB cascade constraints;
drop table if exists ACT_RU_SUSPENDED_JOB cascade constraints;
drop table if exists ACT_RU_DEADLETTER_JOB cascade constraints;
drop table if exists ACT_RU_HISTORY_JOB cascade constraints;

drop table if exists ACT_RU_TASK cascade constraints;

drop index if exists ACT_IDX_TASK_CREATE;

drop table if exists ACT_HI_TASKINST cascade constraints;
drop table if exists ACT_HI_TSK_LOG cascade constraints;

drop index if exists ACT_IDX_HI_TASK_SCOPE;
drop index if exists ACT_IDX_HI_TASK_SUB_SCOPE;
drop index if exists ACT_IDX_HI_TASK_SCOPE_DEF;

drop table if exists ACT_RU_VARIABLE cascade constraints;


drop table if exists ACT_HI_VARINST cascade constraints;

drop index if exists ACT_IDX_HI_PROCVAR_NAME_TYPE;
drop index if exists ACT_IDX_HI_VAR_SCOPE_ID_TYPE;
drop index if exists ACT_IDX_HI_VAR_SUB_ID_TYPE;


drop table if exists ACT_GE_BYTEARRAY cascade constraints;
drop table if exists ACT_GE_PROPERTY cascade constraints;

