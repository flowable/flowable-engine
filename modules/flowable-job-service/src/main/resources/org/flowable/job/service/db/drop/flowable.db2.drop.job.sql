drop index ACT_IDX_JOB_SCOPE;
drop index ACT_IDX_JOB_SUB_SCOPE;
drop index ACT_IDX_JOB_SCOPE_DEF;
drop index ACT_IDX_TJOB_SCOPE;
drop index ACT_IDX_TJOB_SUB_SCOPE;
drop index ACT_IDX_TJOB_SCOPE_DEF;
drop index ACT_IDX_SJOB_SCOPE;
drop index ACT_IDX_SJOB_SUB_SCOPE;
drop index ACT_IDX_SJOB_SCOPE_DEF;
drop index ACT_IDX_DJOB_SCOPE;
drop index ACT_IDX_DJOB_SUB_SCOPE;
drop index ACT_IDX_DJOB_SCOPE_DEF;
drop index ACT_IDX_EJOB_SCOPE;
drop index ACT_IDX_EJOB_SUB_SCOPE;
drop index ACT_IDX_EJOB_SCOPE_DEF;

alter table ACT_RU_JOB
    drop foreign key ACT_FK_JOB_EXCEPTION;

alter table ACT_RU_JOB
    drop foreign key ACT_FK_JOB_CUSTOM_VAL;

alter table ACT_RU_TIMER_JOB
    drop foreign key ACT_FK_TJOB_EXCEPTION;

alter table ACT_RU_TIMER_JOB
    drop foreign key ACT_FK_TJOB_CUSTOM_VAL;

alter table ACT_RU_SUSPENDED_JOB
    drop foreign key ACT_FK_SJOB_EXCEPTION;

alter table ACT_RU_SUSPENDED_JOB
    drop foreign key ACT_FK_SJOB_CUSTOM_VAL;

alter table ACT_RU_DEADLETTER_JOB
    drop foreign key ACT_FK_DJOB_EXCEPTION;

alter table ACT_RU_DEADLETTER_JOB
    drop foreign key ACT_FK_DJOB_CUSTOM_VAL;

alter table ACT_RU_EXTERNAL_JOB
    drop foreign key ACT_FK_EJOB_EXCEPTION;

alter table ACT_RU_EXTERNAL_JOB
    drop foreign key ACT_FK_EJOB_CUSTOM_VAL;

drop table ACT_RU_JOB;
drop table ACT_RU_TIMER_JOB;
drop table ACT_RU_SUSPENDED_JOB;
drop table ACT_RU_DEADLETTER_JOB;
drop table ACT_RU_HISTORY_JOB;
drop table ACT_RU_EXTERNAL_JOB;
