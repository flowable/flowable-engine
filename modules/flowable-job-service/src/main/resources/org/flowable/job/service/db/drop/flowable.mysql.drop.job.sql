drop index ACT_IDX_JOB_SCOPE on ACT_RU_JOB;
drop index ACT_IDX_JOB_SUB_SCOPE on ACT_RU_JOB;
drop index ACT_IDX_JOB_SCOPE_DEF on ACT_RU_JOB;
drop index ACT_IDX_TJOB_SCOPE on ACT_RU_TIMER_JOB;
drop index ACT_IDX_TJOB_SUB_SCOPE on ACT_RU_TIMER_JOB;
drop index ACT_IDX_TJOB_SCOPE_DEF on ACT_RU_TIMER_JOB;
drop index ACT_IDX_SJOB_SCOPE on ACT_RU_SUSPENDED_JOB;
drop index ACT_IDX_SJOB_SUB_SCOPE on ACT_RU_SUSPENDED_JOB;
drop index ACT_IDX_SJOB_SCOPE_DEF on ACT_RU_SUSPENDED_JOB;
drop index ACT_IDX_DJOB_SCOPE on ACT_RU_DEADLETTER_JOB;
drop index ACT_IDX_DJOB_SUB_SCOPE on ACT_RU_DEADLETTER_JOB;
drop index ACT_IDX_DJOB_SCOPE_DEF on ACT_RU_DEADLETTER_JOB;   

alter table ACT_RU_JOB
    drop foreign key ACT_FK_JOB_EXCEPTION;

alter table ACT_RU_JOB
    drop foreign key ACT_FK_JOB_CUSTOM_VALUES;

alter table ACT_RU_TIMER_JOB
    drop foreign key ACT_FK_TIMER_JOB_EXCEPTION;

alter table ACT_RU_TIMER_JOB
    drop foreign key ACT_FK_TIMER_JOB_CUSTOM_VALUES;

alter table ACT_RU_SUSPENDED_JOB
    drop foreign key ACT_FK_SUSPENDED_JOB_EXCEPTION;

alter table ACT_RU_SUSPENDED_JOB
    drop foreign key ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES;

alter table ACT_RU_DEADLETTER_JOB
    drop foreign key ACT_FK_DEADLETTER_JOB_EXCEPTION;

alter table ACT_RU_DEADLETTER_JOB
    drop foreign key ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES;

drop table if exists ACT_RU_JOB;
drop table if exists ACT_RU_TIMER_JOB;
drop table if exists ACT_RU_SUSPENDED_JOB;
drop table if exists ACT_RU_DEADLETTER_JOB;
drop table if exists ACT_RU_HISTORY_JOB;