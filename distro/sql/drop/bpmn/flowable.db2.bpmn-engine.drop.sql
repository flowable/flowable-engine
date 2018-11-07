drop index ACT_IDX_EXEC_BUSKEY;
drop index ACT_IDX_VARIABLE_TASK_ID;

alter table ACT_GE_BYTEARRAY 
    drop foreign key ACT_FK_BYTEARR_DEPL;

alter table ACT_RU_EXECUTION
    drop foreign key ACT_FK_EXE_PROCINST;

alter table ACT_RU_EXECUTION 
    drop foreign key ACT_FK_EXE_PARENT;

alter table ACT_RU_EXECUTION 
    drop foreign key ACT_FK_EXE_SUPER;
    
alter table ACT_RU_EXECUTION 
    drop foreign key ACT_FK_EXE_PROCDEF;

alter table ACT_RU_IDENTITYLINK
    drop foreign key ACT_FK_TSKASS_TASK;
    
alter table ACT_RU_IDENTITYLINK
    drop foreign key ACT_FK_IDL_PROCINST;  

alter table ACT_RU_IDENTITYLINK
    drop foreign key ACT_FK_ATHRZ_PROCEDEF;

alter table ACT_RU_TASK
	drop foreign key ACT_FK_TASK_EXE;

alter table ACT_RU_TASK
	drop foreign key ACT_FK_TASK_PROCINST;
	
alter table ACT_RU_TASK
	drop foreign key ACT_FK_TASK_PROCDEF;
    
alter table ACT_RU_VARIABLE
    drop foreign key ACT_FK_VAR_EXE;
    
alter table ACT_RU_VARIABLE
	drop foreign key ACT_FK_VAR_PROCINST;    

alter table ACT_RU_JOB 
    drop foreign key ACT_FK_JOB_EXECUTION;
    
alter table ACT_RU_JOB 
    drop foreign key ACT_FK_JOB_PROCESS_INSTANCE;
    
alter table ACT_RU_JOB 
    drop foreign key ACT_FK_JOB_PROC_DEF;

alter table ACT_RU_TIMER_JOB 
    drop foreign key ACT_FK_TIMER_JOB_EXECUTION;
    
alter table ACT_RU_TIMER_JOB 
    drop foreign key ACT_FK_TIMER_JOB_PROCESS_INSTANCE;
    
alter table ACT_RU_TIMER_JOB 
    drop foreign key ACT_FK_TIMER_JOB_PROC_DEF;
    
alter table ACT_RU_SUSPENDED_JOB 
    drop foreign key ACT_FK_SUSPENDED_JOB_EXECUTION;
    
alter table ACT_RU_SUSPENDED_JOB 
    drop foreign key ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE;
    
alter table ACT_RU_SUSPENDED_JOB 
    drop foreign key ACT_FK_SUSPENDED_JOB_PROC_DEF;
    
alter table ACT_RU_DEADLETTER_JOB 
    drop foreign key ACT_FK_DEADLETTER_JOB_EXECUTION;
    
alter table ACT_RU_DEADLETTER_JOB 
    drop foreign key ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE;
    
alter table ACT_RU_DEADLETTER_JOB 
    drop foreign key ACT_FK_DEADLETTER_JOB_PROC_DEF;
    
alter table ACT_RU_EVENT_SUBSCR
    drop foreign key ACT_FK_EVENT_EXEC;

alter table ACT_RE_MODEL 
    drop foreign key ACT_FK_MODEL_SOURCE;

alter table ACT_RE_MODEL 
    drop foreign key ACT_FK_MODEL_SOURCE_EXTRA; 
    
alter table ACT_RE_MODEL 
    drop foreign key ACT_FK_MODEL_DEPLOYMENT;

alter table ACT_PROCDEF_INFO 
	drop foreign key ACT_FK_INFO_JSON_BA;

alter table ACT_PROCDEF_INFO 
	drop foreign key ACT_FK_INFO_PROCDEF;

drop index ACT_IDX_EVENT_SUBSCR_CONFIG_;
drop index ACT_IDX_ATHRZ_PROCEDEF;

drop table ACT_RE_DEPLOYMENT;
drop table ACT_RE_MODEL;
drop table ACT_RE_PROCDEF;
drop table ACT_RU_EXECUTION;
drop table ACT_RU_EVENT_SUBSCR;
drop table ACT_EVT_LOG;
drop table ACT_PROCDEF_INFO;

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
drop index ACT_IDX_RU_VAR_SCOPE_ID_TYPE;
drop index ACT_IDX_RU_VAR_SUB_ID_TYPE;

alter table ACT_RU_VARIABLE
    drop foreign key ACT_FK_VAR_BYTEARRAY;

drop table ACT_RU_VARIABLE;

drop index ACT_IDX_TASK_CREATE;
drop index ACT_IDX_TASK_SCOPE;
drop index ACT_IDX_TASK_SUB_SCOPE;
drop index ACT_IDX_TASK_SCOPE_DEF;

drop table ACT_RU_TASK;
drop index ACT_IDX_IDENT_LNK_USER;
drop index ACT_IDX_IDENT_LNK_GROUP;
drop index ACT_IDX_IDENT_LNK_SCOPE;
drop index ACT_IDX_IDENT_LNK_SCOPE_DEF;

drop table ACT_RU_IDENTITYLINK;
drop table ACT_GE_BYTEARRAY;
drop table ACT_GE_PROPERTY;
