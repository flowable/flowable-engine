drop table if exists ACT_RU_ACTINST cascade;
drop table if exists ACT_RE_DEPLOYMENT cascade;
drop table if exists ACT_RE_MODEL cascade;
drop table if exists ACT_RU_EXECUTION cascade;
drop table if exists ACT_RE_PROCDEF cascade;
drop table if exists ACT_RU_EVENT_SUBSCR cascade;
drop table if exists ACT_EVT_LOG cascade;
drop table if exists ACT_PROCDEF_INFO cascade;

drop index if exists ACT_IDX_EXEC_BUSKEY;
drop index if exists ACT_IDX_VARIABLE_TASK_ID;
drop index if exists ACT_IDX_EVENT_SUBSCR_CONFIG_;
drop index if exists ACT_IDX_ATHRZ_PROCEDEF;
drop index if exists ACT_IDX_INFO_PROCDEF;

drop index if exists ACT_IDX_RU_ACT_INST_START;
drop index if exists ACT_IDX_RU_ACT_INST_END;
drop index if exists ACT_IDX_RU_ACT_INST_PROCINST;
drop index if exists ACT_IDX_RU_ACT_INST_EXEC;
drop index if exists ACT_IDX_ACTINST_EXECUTION;