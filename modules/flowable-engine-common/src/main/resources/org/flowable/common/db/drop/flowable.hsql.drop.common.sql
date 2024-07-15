drop table if exists FLW_RU_BATCH_PART cascade constraints;
drop table if exists FLW_RU_BATCH cascade constraints;

drop index if exists FLW_IDX_BATCH_PART;

drop table if exists ACT_RU_ENTITYLINK cascade;

drop index if exists ACT_IDX_ENT_LNK_SCOPE;
drop index if exists ACT_IDX_ENT_LNK_SCOPE_DEF;

drop table if exists ACT_RU_EVENT_SUBSCR cascade;

drop index if exists ACT_IDX_EVENT_SUBSCR_CONFIG_;
drop index if exists ACT_IDX_EVENT_SUBSCR_SCOPEREF_;

drop table if exists ACT_RU_IDENTITYLINK cascade;

drop index if exists ACT_IDX_IDENT_LNK_USER;
drop index if exists ACT_IDX_IDENT_LNK_GROUP;
drop index if exists ACT_IDX_IDENT_LNK_SCOPE;
drop index if exists ACT_IDX_IDENT_LNK_SUB_SCOPE;
drop index if exists ACT_IDX_IDENT_LNK_SCOPE_DEF;

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

drop table if exists ACT_RU_JOB cascade;
drop table if exists ACT_RU_TIMER_JOB cascade;
drop table if exists ACT_RU_SUSPENDED_JOB cascade;
drop table if exists ACT_RU_DEADLETTER_JOB cascade;
drop table if exists ACT_RU_EXTERNAL_JOB cascade;
drop table if exists ACT_RU_HISTORY_JOB cascade;

drop table if exists ACT_RU_TASK cascade;

drop index if exists ACT_IDX_TASK_CREATE;
drop index if exists ACT_IDX_TASK_SCOPE;
drop index if exists ACT_IDX_TASK_SUB_SCOPE;
drop index if exists ACT_IDX_TASK_SCOPE_DEF;

drop table if exists ACT_RU_VARIABLE cascade;


drop table if exists ACT_GE_BYTEARRAY cascade;
drop table if exists ACT_GE_PROPERTY cascade;

