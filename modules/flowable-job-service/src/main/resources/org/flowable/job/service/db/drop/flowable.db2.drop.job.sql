alter table ACT_RU_JOB 
    drop foreign key ACT_FK_JOB_EXCEPTION;
    
alter table ACT_RU_TIMER_JOB 
    drop foreign key ACT_FK_TIMER_JOB_EXCEPTION;
    
alter table ACT_RU_SUSPENDED_JOB 
    drop foreign key ACT_FK_SUSPENDED_JOB_EXCEPTION;
    
alter table ACT_RU_DEADLETTER_JOB 
    drop foreign key ACT_FK_DEADLETTER_JOB_EXCEPTION;
    
drop table ACT_RU_JOB;
drop table ACT_RU_TIMER_JOB;
drop table ACT_RU_SUSPENDED_JOB;
drop table ACT_RU_DEADLETTER_JOB;
drop table ACT_RU_HISTORY_JOB;                