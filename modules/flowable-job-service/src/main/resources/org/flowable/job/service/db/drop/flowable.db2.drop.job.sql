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

drop table ACT_RU_JOB;
drop table ACT_RU_TIMER_JOB;
drop table ACT_RU_SUSPENDED_JOB;
drop table ACT_RU_DEADLETTER_JOB;
drop table ACT_RU_HISTORY_JOB;