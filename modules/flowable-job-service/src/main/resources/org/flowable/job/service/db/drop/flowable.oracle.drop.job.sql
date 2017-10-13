drop index ACT_IDX_JOB_EXCEPTION;
drop index ACT_IDX_TJOB_EXCEPTION;  
drop index ACT_IDX_SJOB_EXCEPTION;    
drop index ACT_IDX_DJOB_EXCEPTION;

alter table ACT_RU_JOB 
    drop CONSTRAINT ACT_FK_JOB_EXCEPTION;
    
alter table ACT_RU_TIMER_JOB 
    drop CONSTRAINT ACT_FK_TJOB_EXCEPTION;
    
alter table ACT_RU_SUSPENDED_JOB 
    drop CONSTRAINT ACT_FK_SJOB_EXCEPTION;
    
alter table ACT_RU_DEADLETTER_JOB 
    drop CONSTRAINT ACT_FK_DJOB_EXCEPTION;
    
drop table ACT_RU_JOB;
drop table ACT_RU_TIMER_JOB;
drop table ACT_RU_SUSPENDED_JOB;
drop table ACT_RU_DEADLETTER_JOB;
drop table ACT_RU_HISTORY_JOB;                
