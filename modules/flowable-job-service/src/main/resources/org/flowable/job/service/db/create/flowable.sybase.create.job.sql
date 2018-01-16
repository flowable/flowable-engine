create table ACT_RU_JOB (
    ID_ varchar(64) not null,
    REV_ int null,
    TYPE_ varchar(255) not null,
    LOCK_EXP_TIME_ datetime null,
    LOCK_OWNER_ varchar(255) null,
    EXCLUSIVE_ bit not null,
    EXECUTION_ID_ varchar(64) null,
    PROCESS_INSTANCE_ID_ varchar(64) null,
    PROC_DEF_ID_ varchar(64) null,
    SCOPE_ID_ varchar(255) null,
    SUB_SCOPE_ID_ varchar(255) null,
    SCOPE_TYPE_ varchar(255) null,
    SCOPE_DEFINITION_ID_ varchar(255) null,
    RETRIES_ int null,
    EXCEPTION_STACK_ID_ varchar(64) null,
    EXCEPTION_MSG_ varchar(4000) null,
    DUEDATE_ datetime null,
    REPEAT_ varchar(255) null,
    HANDLER_TYPE_ varchar(255) null,
    HANDLER_CFG_ varchar(4000) null,
    CUSTOM_VALUES_ID_ varchar(64) null,
    CREATE_TIME_ datetime null,
    TENANT_ID_ varchar(255) default '' null,
    primary key (ID_)
);

create table ACT_RU_TIMER_JOB (
    ID_ varchar(64) not null,
    REV_ int null,
    TYPE_ varchar(255) not null,
    LOCK_EXP_TIME_ datetime null,
    LOCK_OWNER_ varchar(255) null,
    EXCLUSIVE_ bit not null,
    EXECUTION_ID_ varchar(64) null,
    PROCESS_INSTANCE_ID_ varchar(64) null,
    PROC_DEF_ID_ varchar(64) null,
    SCOPE_ID_ varchar(255) null,
    SUB_SCOPE_ID_ varchar(255) null,
    SCOPE_TYPE_ varchar(255) null,
    SCOPE_DEFINITION_ID_ varchar(255) null,
    RETRIES_ int null,
    EXCEPTION_STACK_ID_ varchar(64) null,
    EXCEPTION_MSG_ varchar(4000) null,
    DUEDATE_ datetime NULL,
    REPEAT_ varchar(255) null,
    HANDLER_TYPE_ varchar(255) null,
    HANDLER_CFG_ varchar(4000) null,
    CUSTOM_VALUES_ID_ varchar(64) null,
    CREATE_TIME_ datetime NULL,
    TENANT_ID_ varchar(255) default '' null,
    primary key (ID_)
);

create table ACT_RU_SUSPENDED_JOB (
    ID_ varchar(64) not null,
    REV_ int null,
    TYPE_ varchar(255) not null,
    EXCLUSIVE_ bit not null,
    EXECUTION_ID_ varchar(64) null,
    PROCESS_INSTANCE_ID_ varchar(64) null,
    PROC_DEF_ID_ varchar(64) null,
    SCOPE_ID_ varchar(255) null,
    SUB_SCOPE_ID_ varchar(255) null,
    SCOPE_TYPE_ varchar(255) null,
    SCOPE_DEFINITION_ID_ varchar(255) null,
    RETRIES_ int null,
    EXCEPTION_STACK_ID_ varchar(64) null,
    EXCEPTION_MSG_ varchar(4000) null,
    DUEDATE_ datetime NULL,
    REPEAT_ varchar(255) null,
    HANDLER_TYPE_ varchar(255) null,
    HANDLER_CFG_ varchar(4000) null,
    CUSTOM_VALUES_ID_ varchar(64) null,
    CREATE_TIME_ datetime NULL,
    TENANT_ID_ varchar(255) default '' null,
    primary key (ID_)
);

create table ACT_RU_DEADLETTER_JOB (
    ID_ varchar(64) not null,
    REV_ int null,
    TYPE_ varchar(255) not null,
    EXCLUSIVE_ bit not null,
    EXECUTION_ID_ varchar(64) null,
    PROCESS_INSTANCE_ID_ varchar(64) null,
    PROC_DEF_ID_ varchar(64) null,
    SCOPE_ID_ varchar(255) null,
    SUB_SCOPE_ID_ varchar(255) null,
    SCOPE_TYPE_ varchar(255) null,
    SCOPE_DEFINITION_ID_ varchar(255) null,
    EXCEPTION_STACK_ID_ varchar(64) null,
    EXCEPTION_MSG_ varchar(4000) null,
    DUEDATE_ datetime NULL,
    REPEAT_ varchar(255) null,
    HANDLER_TYPE_ varchar(255) null,
    HANDLER_CFG_ varchar(4000) null,
    CUSTOM_VALUES_ID_ varchar(64) null,
    CREATE_TIME_ datetime NULL,
    TENANT_ID_ varchar(255) default '' null,
    primary key (ID_)
);

create table ACT_RU_HISTORY_JOB (
    ID_ varchar(64) not null,
    REV_ int null,
    LOCK_EXP_TIME_ datetime NULL,
    LOCK_OWNER_ varchar(255) null,
    RETRIES_ int null,
    EXCEPTION_STACK_ID_ varchar(64) null,
    EXCEPTION_MSG_ varchar(4000) null,
    HANDLER_TYPE_ varchar(255) null,
    HANDLER_CFG_ varchar(4000) null,
    CUSTOM_VALUES_ID_ nvarchar(64) null,
    ADV_HANDLER_CFG_ID_ varchar(64) null,
    CREATE_TIME_ datetime NULL,
    TENANT_ID_ varchar(255) default '' null,
    primary key (ID_)
);

create index ACT_IDX_JOB_EXCEPTION_STACK_ID on ACT_RU_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_JOB_CUSTOM_VALUES_ID on ACT_RU_JOB(CUSTOM_VALUES_ID_);

create index ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID on ACT_RU_TIMER_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID on ACT_RU_TIMER_JOB(CUSTOM_VALUES_ID_);

create index ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID on ACT_RU_SUSPENDED_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID on ACT_RU_SUSPENDED_JOB(CUSTOM_VALUES_ID_);

create index ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID on ACT_RU_DEADLETTER_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID on ACT_RU_DEADLETTER_JOB(CUSTOM_VALUES_ID_);

alter table ACT_RU_JOB
    add constraint ACT_FK_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_JOB
    add constraint ACT_FK_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_TIMER_JOB
    add constraint ACT_FK_TIMER_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_TIMER_JOB
    add constraint ACT_FK_TIMER_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_SUSPENDED_JOB
    add constraint ACT_FK_SUSPENDED_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_SUSPENDED_JOB
    add constraint ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_DEADLETTER_JOB
    add constraint ACT_FK_DEADLETTER_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_DEADLETTER_JOB
    add constraint ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_JOB_SCOPE on ACT_RU_JOB(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_JOB_SUB_SCOPE on ACT_RU_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_JOB_SCOPE_DEF on ACT_RU_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_TJOB_SCOPE on ACT_RU_TIMER_JOB(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TJOB_SUB_SCOPE on ACT_RU_TIMER_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TJOB_SCOPE_DEF on ACT_RU_TIMER_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_); 

create index ACT_IDX_SJOB_SCOPE on ACT_RU_SUSPENDED_JOB(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_SJOB_SUB_SCOPE on ACT_RU_SUSPENDED_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_SJOB_SCOPE_DEF on ACT_RU_SUSPENDED_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);   

create index ACT_IDX_DJOB_SCOPE on ACT_RU_DEADLETTER_JOB(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_DJOB_SUB_SCOPE on ACT_RU_DEADLETTER_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_DJOB_SCOPE_DEF on ACT_RU_DEADLETTER_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_); 

insert into ACT_GE_PROPERTY values ('job.schema.version', '6.3.0.0', 1);