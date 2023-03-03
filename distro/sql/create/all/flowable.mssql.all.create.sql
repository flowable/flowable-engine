create table ACT_GE_PROPERTY (
    NAME_ nvarchar(64),
    VALUE_ nvarchar(300),
    REV_ int,
    primary key (NAME_)
);

create table ACT_GE_BYTEARRAY (
    ID_ nvarchar(64),
    REV_ int,
    NAME_ nvarchar(255),
    DEPLOYMENT_ID_ nvarchar(64),
    BYTES_  varbinary(max),
    GENERATED_ tinyint,
    primary key (ID_)
);

insert into ACT_GE_PROPERTY
values ('common.schema.version', '6.8.0.0', 1);

insert into ACT_GE_PROPERTY
values ('next.dbid', '1', 1);


create table ACT_RU_ENTITYLINK (
    ID_ nvarchar(64),
    REV_ int,
    CREATE_TIME_ datetime,
    LINK_TYPE_ nvarchar(255),
    SCOPE_ID_ nvarchar(255),
    SUB_SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    PARENT_ELEMENT_ID_ nvarchar(255),
    REF_SCOPE_ID_ nvarchar(255),
    REF_SCOPE_TYPE_ nvarchar(255),
    REF_SCOPE_DEFINITION_ID_ nvarchar(255),
    ROOT_SCOPE_ID_ nvarchar(255),
    ROOT_SCOPE_TYPE_ nvarchar(255),
    HIERARCHY_TYPE_ nvarchar(255),
    primary key (ID_)
);

create index ACT_IDX_ENT_LNK_SCOPE on ACT_RU_ENTITYLINK(SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_ENT_LNK_REF_SCOPE on ACT_RU_ENTITYLINK(REF_SCOPE_ID_, REF_SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_ENT_LNK_ROOT_SCOPE on ACT_RU_ENTITYLINK(ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_ENT_LNK_SCOPE_DEF on ACT_RU_ENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);

insert into ACT_GE_PROPERTY values ('entitylink.schema.version', '6.8.0.0', 1);

create table ACT_HI_ENTITYLINK (
    ID_ nvarchar(64),
    LINK_TYPE_ nvarchar(255),
    CREATE_TIME_ datetime,
    SCOPE_ID_ nvarchar(255),
    SUB_SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    PARENT_ELEMENT_ID_ nvarchar(255),
    REF_SCOPE_ID_ nvarchar(255),
    REF_SCOPE_TYPE_ nvarchar(255),
    REF_SCOPE_DEFINITION_ID_ nvarchar(255),
    ROOT_SCOPE_ID_ nvarchar(255),
    ROOT_SCOPE_TYPE_ nvarchar(255),
    HIERARCHY_TYPE_ nvarchar(255),
    primary key (ID_)
);

create index ACT_IDX_HI_ENT_LNK_SCOPE on ACT_HI_ENTITYLINK(SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_HI_ENT_LNK_REF_SCOPE on ACT_HI_ENTITYLINK(REF_SCOPE_ID_, REF_SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_HI_ENT_LNK_ROOT_SCOPE on ACT_HI_ENTITYLINK(ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_HI_ENT_LNK_SCOPE_DEF on ACT_HI_ENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);


create table ACT_RU_IDENTITYLINK (
    ID_ nvarchar(64),
    REV_ int,
    GROUP_ID_ nvarchar(255),
    TYPE_ nvarchar(255),
    USER_ID_ nvarchar(255),
    TASK_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    PROC_DEF_ID_ nvarchar(64),
    SCOPE_ID_ nvarchar(255),
    SUB_SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    primary key (ID_)
);

create index ACT_IDX_IDENT_LNK_USER on ACT_RU_IDENTITYLINK(USER_ID_);
create index ACT_IDX_IDENT_LNK_GROUP on ACT_RU_IDENTITYLINK(GROUP_ID_);
create index ACT_IDX_IDENT_LNK_SCOPE on ACT_RU_IDENTITYLINK(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_IDENT_LNK_SUB_SCOPE on ACT_RU_IDENTITYLINK(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_IDENT_LNK_SCOPE_DEF on ACT_RU_IDENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('identitylink.schema.version', '6.8.0.0', 1);

create table ACT_HI_IDENTITYLINK (
    ID_ nvarchar(64),
    GROUP_ID_ nvarchar(255),
    TYPE_ nvarchar(255),
    USER_ID_ nvarchar(255),
    TASK_ID_ nvarchar(64),
    CREATE_TIME_ datetime,
    PROC_INST_ID_ nvarchar(64),
    SCOPE_ID_ nvarchar(255),
    SUB_SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    primary key (ID_)
);

create index ACT_IDX_HI_IDENT_LNK_USER on ACT_HI_IDENTITYLINK(USER_ID_);
create index ACT_IDX_HI_IDENT_LNK_SCOPE on ACT_HI_IDENTITYLINK(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_IDENT_LNK_SUB_SCOPE on ACT_HI_IDENTITYLINK(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_IDENT_LNK_SCOPE_DEF on ACT_HI_IDENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);


create table ACT_RU_JOB (
    ID_ nvarchar(64) NOT NULL,
    REV_ int,
    CATEGORY_ varchar(255),
    TYPE_ nvarchar(255) NOT NULL,
    LOCK_EXP_TIME_ datetime,
    LOCK_OWNER_ nvarchar(255),
    EXCLUSIVE_ bit,
    EXECUTION_ID_ nvarchar(64),
    PROCESS_INSTANCE_ID_ nvarchar(64),
    PROC_DEF_ID_ nvarchar(64),
    ELEMENT_ID_ nvarchar(255),
    ELEMENT_NAME_ nvarchar(255),
    SCOPE_ID_ nvarchar(255),
    SUB_SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    CORRELATION_ID_ nvarchar(255),
    RETRIES_ int,
    EXCEPTION_STACK_ID_ nvarchar(64),
    EXCEPTION_MSG_ nvarchar(4000),
    DUEDATE_ datetime NULL,
    REPEAT_ nvarchar(255),
    HANDLER_TYPE_ nvarchar(255),
    HANDLER_CFG_ nvarchar(4000),
    CUSTOM_VALUES_ID_ nvarchar(64),
    CREATE_TIME_ datetime2 NULL,
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table ACT_RU_TIMER_JOB (
    ID_ nvarchar(64) NOT NULL,
    REV_ int,
    CATEGORY_ varchar(255),
    TYPE_ nvarchar(255) NOT NULL,
    LOCK_EXP_TIME_ datetime,
    LOCK_OWNER_ nvarchar(255),
    EXCLUSIVE_ bit,
    EXECUTION_ID_ nvarchar(64),
    PROCESS_INSTANCE_ID_ nvarchar(64),
    PROC_DEF_ID_ nvarchar(64),
    ELEMENT_ID_ nvarchar(255),
    ELEMENT_NAME_ nvarchar(255),
    SCOPE_ID_ nvarchar(255),
    SUB_SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    CORRELATION_ID_ nvarchar(255),
    RETRIES_ int,
    EXCEPTION_STACK_ID_ nvarchar(64),
    EXCEPTION_MSG_ nvarchar(4000),
    DUEDATE_ datetime NULL,
    REPEAT_ nvarchar(255),
    HANDLER_TYPE_ nvarchar(255),
    HANDLER_CFG_ nvarchar(4000),
    CUSTOM_VALUES_ID_ nvarchar(64),
    CREATE_TIME_ datetime2 NULL,
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table ACT_RU_SUSPENDED_JOB (
    ID_ nvarchar(64) NOT NULL,
    REV_ int,
    CATEGORY_ varchar(255),
    TYPE_ nvarchar(255) NOT NULL,
    EXCLUSIVE_ bit,
    EXECUTION_ID_ nvarchar(64),
    PROCESS_INSTANCE_ID_ nvarchar(64),
    PROC_DEF_ID_ nvarchar(64),
    ELEMENT_ID_ nvarchar(255),
    ELEMENT_NAME_ nvarchar(255),
    SCOPE_ID_ nvarchar(255),
    SUB_SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    CORRELATION_ID_ nvarchar(255),
    RETRIES_ int,
    EXCEPTION_STACK_ID_ nvarchar(64),
    EXCEPTION_MSG_ nvarchar(4000),
    DUEDATE_ datetime NULL,
    REPEAT_ nvarchar(255),
    HANDLER_TYPE_ nvarchar(255),
    HANDLER_CFG_ nvarchar(4000),
    CUSTOM_VALUES_ID_ nvarchar(64),
    CREATE_TIME_ datetime2 NULL,
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table ACT_RU_DEADLETTER_JOB (
    ID_ nvarchar(64) NOT NULL,
    REV_ int,
    CATEGORY_ varchar(255),
    TYPE_ nvarchar(255) NOT NULL,
    EXCLUSIVE_ bit,
    EXECUTION_ID_ nvarchar(64),
    PROCESS_INSTANCE_ID_ nvarchar(64),
    PROC_DEF_ID_ nvarchar(64),
    ELEMENT_ID_ nvarchar(255),
    ELEMENT_NAME_ nvarchar(255),
    SCOPE_ID_ nvarchar(255),
    SUB_SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    CORRELATION_ID_ nvarchar(255),
    EXCEPTION_STACK_ID_ nvarchar(64),
    EXCEPTION_MSG_ nvarchar(4000),
    DUEDATE_ datetime NULL,
    REPEAT_ nvarchar(255),
    HANDLER_TYPE_ nvarchar(255),
    HANDLER_CFG_ nvarchar(4000),
    CUSTOM_VALUES_ID_ nvarchar(64),
    CREATE_TIME_ datetime2 NULL,
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table ACT_RU_HISTORY_JOB (
    ID_ nvarchar(64) NOT NULL,
    REV_ int,
    LOCK_EXP_TIME_ datetime NULL,
    LOCK_OWNER_ nvarchar(255),
    RETRIES_ int,
    EXCEPTION_STACK_ID_ nvarchar(64),
    EXCEPTION_MSG_ nvarchar(4000),
    HANDLER_TYPE_ nvarchar(255),
    HANDLER_CFG_ nvarchar(4000),
    CUSTOM_VALUES_ID_ nvarchar(64),
    ADV_HANDLER_CFG_ID_ nvarchar(64),
    CREATE_TIME_ datetime2 NULL,
    SCOPE_TYPE_ nvarchar(255),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table ACT_RU_EXTERNAL_JOB (
    ID_ nvarchar(64) NOT NULL,
    REV_ int,
    CATEGORY_ varchar(255),
    TYPE_ nvarchar(255) NOT NULL,
    LOCK_EXP_TIME_ datetime,
    LOCK_OWNER_ nvarchar(255),
    EXCLUSIVE_ bit,
    EXECUTION_ID_ nvarchar(64),
    PROCESS_INSTANCE_ID_ nvarchar(64),
    PROC_DEF_ID_ nvarchar(64),
    ELEMENT_ID_ nvarchar(255),
    ELEMENT_NAME_ nvarchar(255),
    SCOPE_ID_ nvarchar(255),
    SUB_SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    CORRELATION_ID_ nvarchar(255),
    RETRIES_ int,
    EXCEPTION_STACK_ID_ nvarchar(64),
    EXCEPTION_MSG_ nvarchar(4000),
    DUEDATE_ datetime NULL,
    REPEAT_ nvarchar(255),
    HANDLER_TYPE_ nvarchar(255),
    HANDLER_CFG_ nvarchar(4000),
    CUSTOM_VALUES_ID_ nvarchar(64),
    CREATE_TIME_ datetime2 NULL,
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create index ACT_IDX_JOB_EXCEPTION_STACK_ID on ACT_RU_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_JOB_CUSTOM_VALUES_ID on ACT_RU_JOB(CUSTOM_VALUES_ID_);
create index ACT_IDX_JOB_CORRELATION_ID on ACT_RU_JOB(CORRELATION_ID_);

create index ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID on ACT_RU_TIMER_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID on ACT_RU_TIMER_JOB(CUSTOM_VALUES_ID_);
create index ACT_IDX_TIMER_JOB_CORRELATION_ID on ACT_RU_TIMER_JOB(CORRELATION_ID_);
create index ACT_IDX_TIMER_JOB_DUEDATE on ACT_RU_TIMER_JOB(DUEDATE_); 

create index ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID on ACT_RU_SUSPENDED_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID on ACT_RU_SUSPENDED_JOB(CUSTOM_VALUES_ID_);
create index ACT_IDX_SUSPENDED_JOB_CORRELATION_ID on ACT_RU_SUSPENDED_JOB(CORRELATION_ID_);

create index ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID on ACT_RU_DEADLETTER_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID on ACT_RU_DEADLETTER_JOB(CUSTOM_VALUES_ID_);
create index ACT_IDX_DEADLETTER_JOB_CORRELATION_ID on ACT_RU_DEADLETTER_JOB(CORRELATION_ID_);

create index ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID on ACT_RU_EXTERNAL_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID on ACT_RU_EXTERNAL_JOB(CUSTOM_VALUES_ID_);
create index ACT_IDX_EXTERNAL_JOB_CORRELATION_ID on ACT_RU_EXTERNAL_JOB(CORRELATION_ID_);

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

alter table ACT_RU_EXTERNAL_JOB
    add constraint ACT_FK_EXTERNAL_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_EXTERNAL_JOB
    add constraint ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES
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

create index ACT_IDX_EJOB_SCOPE on ACT_RU_EXTERNAL_JOB(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_EJOB_SUB_SCOPE on ACT_RU_EXTERNAL_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_EJOB_SCOPE_DEF on ACT_RU_EXTERNAL_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('job.schema.version', '6.8.0.0', 1);

create table FLW_RU_BATCH (
    ID_ nvarchar(64) not null,
    REV_ int,
    TYPE_ nvarchar(64) not null,
    SEARCH_KEY_ nvarchar(255),
    SEARCH_KEY2_ nvarchar(255),
    CREATE_TIME_ datetime not null,
    COMPLETE_TIME_ datetime,
    STATUS_ nvarchar(255),
    BATCH_DOC_ID_ nvarchar(64),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table FLW_RU_BATCH_PART (
    ID_ nvarchar(64) not null,
    REV_ int,
    BATCH_ID_ nvarchar(64),
    TYPE_ nvarchar(64) not null,
    SCOPE_ID_ nvarchar(64),
    SUB_SCOPE_ID_ nvarchar(64),
    SCOPE_TYPE_ nvarchar(64),
    SEARCH_KEY_ nvarchar(255),
    SEARCH_KEY2_ nvarchar(255),
    CREATE_TIME_ datetime not null,
    COMPLETE_TIME_ datetime,
    STATUS_ nvarchar(255),
    RESULT_DOC_ID_ nvarchar(64),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create index FLW_IDX_BATCH_PART on FLW_RU_BATCH_PART(BATCH_ID_);

alter table FLW_RU_BATCH_PART
    add constraint FLW_FK_BATCH_PART_PARENT
    foreign key (BATCH_ID_)
    references FLW_RU_BATCH (ID_);

insert into ACT_GE_PROPERTY values ('batch.schema.version', '6.8.0.0', 1);


create table ACT_RU_TASK (
    ID_ nvarchar(64),
    REV_ int,
    EXECUTION_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    PROC_DEF_ID_ nvarchar(64),
    TASK_DEF_ID_ nvarchar(64),
    SCOPE_ID_ nvarchar(255),
    SUB_SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    PROPAGATED_STAGE_INST_ID_ nvarchar(255),
    NAME_ nvarchar(255),
    PARENT_TASK_ID_ nvarchar(64),
    DESCRIPTION_ nvarchar(4000),
    TASK_DEF_KEY_ nvarchar(255),
    OWNER_ nvarchar(255),
    ASSIGNEE_ nvarchar(255),
    DELEGATION_ nvarchar(64),
    PRIORITY_ int,
    CREATE_TIME_ datetime,
    DUE_DATE_ datetime,
    CATEGORY_ nvarchar(255),
    SUSPENSION_STATE_ int,
    TENANT_ID_ nvarchar(255) default '',
    FORM_KEY_ nvarchar(255),
    CLAIM_TIME_ datetime,
    IS_COUNT_ENABLED_ tinyint,
    VAR_COUNT_ int, 
    ID_LINK_COUNT_ int,
    SUB_TASK_COUNT_ int,
    primary key (ID_)
);

create index ACT_IDX_TASK_CREATE on ACT_RU_TASK(CREATE_TIME_);
create index ACT_IDX_TASK_SCOPE on ACT_RU_TASK(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TASK_SUB_SCOPE on ACT_RU_TASK(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TASK_SCOPE_DEF on ACT_RU_TASK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('task.schema.version', '6.8.0.0', 1);

create table ACT_HI_TASKINST (
    ID_ nvarchar(64) not null,
    REV_ int default 1,
    PROC_DEF_ID_ nvarchar(64),
    TASK_DEF_ID_ nvarchar(64),
    TASK_DEF_KEY_ nvarchar(255),
    PROC_INST_ID_ nvarchar(64),
    EXECUTION_ID_ nvarchar(64),
    SCOPE_ID_ nvarchar(255),
    SUB_SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    PROPAGATED_STAGE_INST_ID_ nvarchar(255),
    NAME_ nvarchar(255),
    PARENT_TASK_ID_ nvarchar(64),
    DESCRIPTION_ nvarchar(4000),
    OWNER_ nvarchar(255),
    ASSIGNEE_ nvarchar(255),
    START_TIME_ datetime not null,
    CLAIM_TIME_ datetime,
    END_TIME_ datetime,
    DURATION_ numeric(19,0),
    DELETE_REASON_ nvarchar(4000),
    PRIORITY_ int,
    DUE_DATE_ datetime,
    FORM_KEY_ nvarchar(255),
    CATEGORY_ nvarchar(255),
    TENANT_ID_ nvarchar(255) default '',
    LAST_UPDATED_TIME_ datetime2,
    primary key (ID_)
);

create table ACT_HI_TSK_LOG (
    ID_ numeric(19,0) IDENTITY (1,1),
    TYPE_ nvarchar(64),
    TASK_ID_ nvarchar(64) not null,
    TIME_STAMP_ datetime not null,
    USER_ID_ nvarchar(255),
    DATA_ nvarchar(4000),
    EXECUTION_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    PROC_DEF_ID_ nvarchar(64),
    SCOPE_ID_ nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    SUB_SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create index ACT_IDX_HI_TASK_SCOPE on ACT_HI_TASKINST(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_TASK_SUB_SCOPE on ACT_HI_TASKINST(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_TASK_SCOPE_DEF on ACT_HI_TASKINST(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create table ACT_RU_VARIABLE (
    ID_ nvarchar(64) not null,
    REV_ int,
    TYPE_ nvarchar(255) not null,
    NAME_ nvarchar(255) not null,
    EXECUTION_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    TASK_ID_ nvarchar(64),
    SCOPE_ID_ nvarchar(255),
    SUB_SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    BYTEARRAY_ID_ nvarchar(64),
    DOUBLE_ double precision,
    LONG_ numeric(19,0),
    TEXT_ nvarchar(4000),
    TEXT2_ nvarchar(4000),
    primary key (ID_)
);

create index ACT_IDX_RU_VAR_SCOPE_ID_TYPE on ACT_RU_VARIABLE(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_RU_VAR_SUB_ID_TYPE on ACT_RU_VARIABLE(SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_VARIABLE_BA on ACT_RU_VARIABLE(BYTEARRAY_ID_);
alter table ACT_RU_VARIABLE 
    add constraint ACT_FK_VAR_BYTEARRAY 
    foreign key (BYTEARRAY_ID_) 
    references ACT_GE_BYTEARRAY (ID_);

insert into ACT_GE_PROPERTY values ('variable.schema.version', '6.8.0.0', 1);

create table ACT_HI_VARINST (
    ID_ nvarchar(64) not null,
    REV_ int default 1,
    PROC_INST_ID_ nvarchar(64),
    EXECUTION_ID_ nvarchar(64),
    TASK_ID_ nvarchar(64),
    NAME_ nvarchar(255) not null,
    VAR_TYPE_ nvarchar(100),
    SCOPE_ID_ nvarchar(255),
    SUB_SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    BYTEARRAY_ID_ nvarchar(64),
    DOUBLE_ double precision,
    LONG_ numeric(19,0),
    TEXT_ nvarchar(4000),
    TEXT2_ nvarchar(4000),
    CREATE_TIME_ datetime,
    LAST_UPDATED_TIME_ datetime2,
    primary key (ID_)
);

create index ACT_IDX_HI_PROCVAR_NAME_TYPE on ACT_HI_VARINST(NAME_, VAR_TYPE_);
create index ACT_IDX_HI_VAR_SCOPE_ID_TYPE on ACT_HI_VARINST(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_VAR_SUB_ID_TYPE on ACT_HI_VARINST(SUB_SCOPE_ID_, SCOPE_TYPE_);


create table ACT_RU_EVENT_SUBSCR (
    ID_ nvarchar(64) not null,
    REV_ int,
    EVENT_TYPE_ nvarchar(255) not null,
    EVENT_NAME_ nvarchar(255),
    EXECUTION_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    ACTIVITY_ID_ nvarchar(64),
    CONFIGURATION_ nvarchar(255),
    CREATED_ datetime not null,
    PROC_DEF_ID_ nvarchar(64),
    SUB_SCOPE_ID_ nvarchar(64),
    SCOPE_ID_ nvarchar(64),
    SCOPE_DEFINITION_ID_ nvarchar(64),
    SCOPE_TYPE_ nvarchar(64),
    LOCK_TIME_ datetime,
    LOCK_OWNER_ nvarchar(255),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create index ACT_IDX_EVENT_SUBSCR_CONFIG_ on ACT_RU_EVENT_SUBSCR(CONFIGURATION_);
create index ACT_IDX_EVENT_SUBSCR_EXEC_ID on ACT_RU_EVENT_SUBSCR(EXECUTION_ID_);
create index ACT_IDX_EVENT_SUBSCR_SCOPEREF_ on ACT_RU_EVENT_SUBSCR(SCOPE_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('eventsubscription.schema.version', '6.8.0.0', 1);
create table ACT_RE_DEPLOYMENT (
    ID_ nvarchar(64),
    NAME_ nvarchar(255),
    CATEGORY_ nvarchar(255),
    KEY_ nvarchar(255),
    TENANT_ID_ nvarchar(255) default '',
    DEPLOY_TIME_ datetime,
    DERIVED_FROM_ nvarchar(64),
    DERIVED_FROM_ROOT_ nvarchar(64),
    PARENT_DEPLOYMENT_ID_ nvarchar(255),
    ENGINE_VERSION_ nvarchar(255),
    primary key (ID_)
);

create table ACT_RE_MODEL (
    ID_ nvarchar(64) not null,
    REV_ int,
    NAME_ nvarchar(255),
    KEY_ nvarchar(255),
    CATEGORY_ nvarchar(255),
    CREATE_TIME_ datetime,
    LAST_UPDATE_TIME_ datetime,
    VERSION_ int,
    META_INFO_ nvarchar(4000),
    DEPLOYMENT_ID_ nvarchar(64),
    EDITOR_SOURCE_VALUE_ID_ nvarchar(64),
    EDITOR_SOURCE_EXTRA_VALUE_ID_ nvarchar(64),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table ACT_RU_EXECUTION (
    ID_ nvarchar(64),
    REV_ int,
    PROC_INST_ID_ nvarchar(64),
    BUSINESS_KEY_ nvarchar(255),
    PARENT_ID_ nvarchar(64),
    PROC_DEF_ID_ nvarchar(64),
    SUPER_EXEC_ nvarchar(64),
    ROOT_PROC_INST_ID_ nvarchar(64),
    ACT_ID_ nvarchar(255),
    IS_ACTIVE_ tinyint,
    IS_CONCURRENT_ tinyint,
    IS_SCOPE_ tinyint,
    IS_EVENT_SCOPE_ tinyint,
    IS_MI_ROOT_ tinyint,
    SUSPENSION_STATE_ tinyint,
    CACHED_ENT_STATE_ int,
    TENANT_ID_ nvarchar(255) default '',
    NAME_ nvarchar(255),
    START_ACT_ID_ nvarchar(255),
    START_TIME_ datetime,
    START_USER_ID_ nvarchar(255),
    LOCK_TIME_ datetime,
    LOCK_OWNER_ nvarchar(255),
    IS_COUNT_ENABLED_ tinyint,
    EVT_SUBSCR_COUNT_ int, 
    TASK_COUNT_ int, 
    JOB_COUNT_ int, 
    TIMER_JOB_COUNT_ int,
    SUSP_JOB_COUNT_ int,
    DEADLETTER_JOB_COUNT_ int,
    EXTERNAL_WORKER_JOB_COUNT_ int,
    VAR_COUNT_ int, 
    ID_LINK_COUNT_ int,
    CALLBACK_ID_ nvarchar(255),
    CALLBACK_TYPE_ nvarchar(255),
    REFERENCE_ID_ nvarchar(255),
    REFERENCE_TYPE_ nvarchar(255),
    PROPAGATED_STAGE_INST_ID_ nvarchar(255),
    BUSINESS_STATUS_ nvarchar(255),
    primary key (ID_)
);

create table ACT_RE_PROCDEF (
    ID_ nvarchar(64) not null,
    REV_ int,
    CATEGORY_ nvarchar(255),
    NAME_ nvarchar(255),
    KEY_ nvarchar(255) not null,
    VERSION_ int not null,
    DEPLOYMENT_ID_ nvarchar(64),
    RESOURCE_NAME_ nvarchar(4000),
    DGRM_RESOURCE_NAME_ nvarchar(4000),
    DESCRIPTION_ nvarchar(4000),
    HAS_START_FORM_KEY_ tinyint,
    HAS_GRAPHICAL_NOTATION_ tinyint,
    SUSPENSION_STATE_ tinyint,
    TENANT_ID_ nvarchar(255) default '',
    DERIVED_FROM_ nvarchar(64),
    DERIVED_FROM_ROOT_ nvarchar(64),
    DERIVED_VERSION_ int not null default 0,
    ENGINE_VERSION_ nvarchar(255),
    primary key (ID_)
);

create table ACT_EVT_LOG (
    LOG_NR_ numeric(19,0) IDENTITY(1,1),
    TYPE_ nvarchar(64),
    PROC_DEF_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    EXECUTION_ID_ nvarchar(64),
    TASK_ID_ nvarchar(64),
    TIME_STAMP_ datetime not null,
    USER_ID_ nvarchar(255),
    DATA_ varbinary(max),
    LOCK_OWNER_ nvarchar(255),
    LOCK_TIME_ datetime null,
    IS_PROCESSED_ tinyint default 0,
    primary key (LOG_NR_)
);

create table ACT_PROCDEF_INFO (
	ID_ nvarchar(64) not null,
    PROC_DEF_ID_ nvarchar(64) not null,
    REV_ int,
    INFO_JSON_ID_ nvarchar(64),
    primary key (ID_)
);

create table ACT_RU_ACTINST (
    ID_ nvarchar(64) not null,
    REV_ int default 1,
    PROC_DEF_ID_ nvarchar(64) not null,
    PROC_INST_ID_ nvarchar(64) not null,
    EXECUTION_ID_ nvarchar(64) not null,
    ACT_ID_ nvarchar(255) not null,
    TASK_ID_ nvarchar(64),
    CALL_PROC_INST_ID_ nvarchar(64),
    ACT_NAME_ nvarchar(255),
    ACT_TYPE_ nvarchar(255) not null,
    ASSIGNEE_ nvarchar(255),
    START_TIME_ datetime not null,
    END_TIME_ datetime,
    DURATION_ numeric(19,0),
    TRANSACTION_ORDER_ int,
    DELETE_REASON_ nvarchar(4000),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create index ACT_IDX_EXEC_BUSKEY on ACT_RU_EXECUTION(BUSINESS_KEY_);
create index ACT_IDX_EXEC_ROOT on ACT_RU_EXECUTION(ROOT_PROC_INST_ID_);
create index ACT_IDX_EXEC_REF_ID_ on ACT_RU_EXECUTION(REFERENCE_ID_);
create index ACT_IDX_VARIABLE_TASK_ID on ACT_RU_VARIABLE(TASK_ID_);
create index ACT_IDX_ATHRZ_PROCEDEF on ACT_RU_IDENTITYLINK(PROC_DEF_ID_);
create index ACT_IDX_EXECUTION_PROC on ACT_RU_EXECUTION(PROC_DEF_ID_);
create index ACT_IDX_EXECUTION_PARENT on ACT_RU_EXECUTION(PARENT_ID_);
create index ACT_IDX_EXECUTION_SUPER on ACT_RU_EXECUTION(SUPER_EXEC_);
create index ACT_IDX_EXECUTION_IDANDREV on ACT_RU_EXECUTION(ID_, REV_);
create index ACT_IDX_VARIABLE_EXEC on ACT_RU_VARIABLE(EXECUTION_ID_);
create index ACT_IDX_VARIABLE_PROCINST on ACT_RU_VARIABLE(PROC_INST_ID_);
create index ACT_IDX_IDENT_LNK_TASK on ACT_RU_IDENTITYLINK(TASK_ID_);
create index ACT_IDX_IDENT_LNK_PROCINST on ACT_RU_IDENTITYLINK(PROC_INST_ID_);
create index ACT_IDX_TASK_EXEC on ACT_RU_TASK(EXECUTION_ID_);
create index ACT_IDX_TASK_PROCINST on ACT_RU_TASK(PROC_INST_ID_);
create index ACT_IDX_EXEC_PROC_INST_ID on ACT_RU_EXECUTION(PROC_INST_ID_);
create index ACT_IDX_TASK_PROC_DEF_ID on ACT_RU_TASK(PROC_DEF_ID_);
create index ACT_IDX_JOB_EXECUTION_ID on ACT_RU_JOB(EXECUTION_ID_);
create index ACT_IDX_JOB_PROCESS_INSTANCE_ID on ACT_RU_JOB(PROCESS_INSTANCE_ID_);
create index ACT_IDX_JOB_PROC_DEF_ID on ACT_RU_JOB(PROC_DEF_ID_);
create index ACT_IDX_TIMER_JOB_EXECUTION_ID on ACT_RU_TIMER_JOB(EXECUTION_ID_);
create index ACT_IDX_TIMER_JOB_PROCESS_INSTANCE_ID on ACT_RU_TIMER_JOB(PROCESS_INSTANCE_ID_);
create index ACT_IDX_TIMER_JOB_PROC_DEF_ID on ACT_RU_TIMER_JOB(PROC_DEF_ID_);
create index ACT_IDX_SUSPENDED_JOB_EXECUTION_ID on ACT_RU_SUSPENDED_JOB(EXECUTION_ID_);
create index ACT_IDX_SUSPENDED_JOB_PROCESS_INSTANCE_ID on ACT_RU_SUSPENDED_JOB(PROCESS_INSTANCE_ID_);
create index ACT_IDX_SUSPENDED_JOB_PROC_DEF_ID on ACT_RU_SUSPENDED_JOB(PROC_DEF_ID_);
create index ACT_IDX_DEADLETTER_JOB_EXECUTION_ID on ACT_RU_DEADLETTER_JOB(EXECUTION_ID_);
create index ACT_IDX_DEADLETTER_JOB_PROCESS_INSTANCE_ID on ACT_RU_DEADLETTER_JOB(PROCESS_INSTANCE_ID_);
create index ACT_IDX_DEADLETTER_JOB_PROC_DEF_ID on ACT_RU_DEADLETTER_JOB(PROC_DEF_ID_);
create index ACT_IDX_INFO_PROCDEF on ACT_PROCDEF_INFO(PROC_DEF_ID_);

create index ACT_IDX_RU_ACTI_START on ACT_RU_ACTINST(START_TIME_);
create index ACT_IDX_RU_ACTI_END on ACT_RU_ACTINST(END_TIME_);
create index ACT_IDX_RU_ACTI_PROC on ACT_RU_ACTINST(PROC_INST_ID_);
create index ACT_IDX_RU_ACTI_PROC_ACT on ACT_RU_ACTINST(PROC_INST_ID_, ACT_ID_);
create index ACT_IDX_RU_ACTI_EXEC on ACT_RU_ACTINST(EXECUTION_ID_);
create index ACT_IDX_RU_ACTI_EXEC_ACT on ACT_RU_ACTINST(EXECUTION_ID_, ACT_ID_);
create index ACT_IDX_RU_ACTI_TASK on ACT_RU_ACTINST(TASK_ID_);

alter table ACT_GE_BYTEARRAY
    add constraint ACT_FK_BYTEARR_DEPL 
    foreign key (DEPLOYMENT_ID_) 
    references ACT_RE_DEPLOYMENT (ID_);

alter table ACT_RE_PROCDEF
    add constraint ACT_UNIQ_PROCDEF
    unique (KEY_,VERSION_, DERIVED_VERSION_, TENANT_ID_);
    
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PARENT 
    foreign key (PARENT_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_SUPER 
    foreign key (SUPER_EXEC_) 
    references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PROCDEF 
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);

alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_TSKASS_TASK 
    foreign key (TASK_ID_) 
    references ACT_RU_TASK (ID_);
    
alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_ATHRZ_PROCEDEF
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);
    
alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_IDL_PROCINST
    foreign key (PROC_INST_ID_) 
    references ACT_RU_EXECUTION (ID_);       
    
alter table ACT_RU_TASK
    add constraint ACT_FK_TASK_EXE
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_TASK
    add constraint ACT_FK_TASK_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_TASK
  	add constraint ACT_FK_TASK_PROCDEF
  	foreign key (PROC_DEF_ID_)
  	references ACT_RE_PROCDEF (ID_);
  
alter table ACT_RU_VARIABLE 
    add constraint ACT_FK_VAR_EXE 
    foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_);

alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION(ID_);

alter table ACT_RU_JOB 
    add constraint ACT_FK_JOB_EXECUTION 
    foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_JOB 
    add constraint ACT_FK_JOB_PROCESS_INSTANCE 
    foreign key (PROCESS_INSTANCE_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_JOB 
    add constraint ACT_FK_JOB_PROC_DEF
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);

alter table ACT_RU_TIMER_JOB 
    add constraint ACT_FK_TIMER_JOB_EXECUTION 
    foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_TIMER_JOB 
    add constraint ACT_FK_TIMER_JOB_PROCESS_INSTANCE 
    foreign key (PROCESS_INSTANCE_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_TIMER_JOB 
    add constraint ACT_FK_TIMER_JOB_PROC_DEF
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);
    
alter table ACT_RU_SUSPENDED_JOB 
    add constraint ACT_FK_SUSPENDED_JOB_EXECUTION 
    foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_SUSPENDED_JOB 
    add constraint ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE 
    foreign key (PROCESS_INSTANCE_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_SUSPENDED_JOB 
    add constraint ACT_FK_SUSPENDED_JOB_PROC_DEF
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);
    
alter table ACT_RU_DEADLETTER_JOB 
    add constraint ACT_FK_DEADLETTER_JOB_EXECUTION 
    foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_DEADLETTER_JOB 
    add constraint ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE 
    foreign key (PROCESS_INSTANCE_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
alter table ACT_RU_DEADLETTER_JOB 
    add constraint ACT_FK_DEADLETTER_JOB_PROC_DEF
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);
    
alter table ACT_RU_EVENT_SUBSCR
    add constraint ACT_FK_EVENT_EXEC
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION(ID_);
    
alter table ACT_RE_MODEL 
    add constraint ACT_FK_MODEL_SOURCE 
    foreign key (EDITOR_SOURCE_VALUE_ID_) 
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RE_MODEL 
    add constraint ACT_FK_MODEL_SOURCE_EXTRA 
    foreign key (EDITOR_SOURCE_EXTRA_VALUE_ID_) 
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RE_MODEL 
    add constraint ACT_FK_MODEL_DEPLOYMENT 
    foreign key (DEPLOYMENT_ID_) 
    references ACT_RE_DEPLOYMENT (ID_);    

alter table ACT_PROCDEF_INFO 
    add constraint ACT_FK_INFO_JSON_BA 
    foreign key (INFO_JSON_ID_) 
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_PROCDEF_INFO 
    add constraint ACT_FK_INFO_PROCDEF 
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);
    
alter table ACT_PROCDEF_INFO
    add constraint ACT_UNIQ_INFO_PROCDEF
    unique (PROC_DEF_ID_);

insert into ACT_GE_PROPERTY
values ('schema.version', '6.8.0.0', 1);

insert into ACT_GE_PROPERTY
values ('schema.history', 'create(6.8.0.0)', 1);

create table ACT_HI_PROCINST (
    ID_ nvarchar(64) not null,
    REV_ int default 1,
    PROC_INST_ID_ nvarchar(64) not null,
    BUSINESS_KEY_ nvarchar(255),
    PROC_DEF_ID_ nvarchar(64) not null,
    START_TIME_ datetime not null,
    END_TIME_ datetime,
    DURATION_ numeric(19,0),
    START_USER_ID_ nvarchar(255),
    START_ACT_ID_ nvarchar(255),
    END_ACT_ID_ nvarchar(255),
    SUPER_PROCESS_INSTANCE_ID_ nvarchar(64),
    DELETE_REASON_ nvarchar(4000),
    TENANT_ID_ nvarchar(255) default '',
    NAME_ nvarchar(255),
    CALLBACK_ID_ nvarchar(255),
    CALLBACK_TYPE_ nvarchar(255),
    REFERENCE_ID_ nvarchar(255),
    REFERENCE_TYPE_ nvarchar(255),
    PROPAGATED_STAGE_INST_ID_ nvarchar(255),
    BUSINESS_STATUS_ nvarchar(255),
    primary key (ID_),
    unique (PROC_INST_ID_)
);

create table ACT_HI_ACTINST (
    ID_ nvarchar(64) not null,
    REV_ int default 1,
    PROC_DEF_ID_ nvarchar(64) not null,
    PROC_INST_ID_ nvarchar(64) not null,
    EXECUTION_ID_ nvarchar(64) not null,
    ACT_ID_ nvarchar(255) not null,
    TASK_ID_ nvarchar(64),
    CALL_PROC_INST_ID_ nvarchar(64),
    ACT_NAME_ nvarchar(255),
    ACT_TYPE_ nvarchar(255) not null,
    ASSIGNEE_ nvarchar(255),
    START_TIME_ datetime not null,
    END_TIME_ datetime,
    TRANSACTION_ORDER_ int,
    DURATION_ numeric(19,0),
    DELETE_REASON_ nvarchar(4000),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table ACT_HI_DETAIL (
    ID_ nvarchar(64) not null,
    TYPE_ nvarchar(255) not null,
    PROC_INST_ID_ nvarchar(64),
    EXECUTION_ID_ nvarchar(64),
    TASK_ID_ nvarchar(64),
    ACT_INST_ID_ nvarchar(64),
    NAME_ nvarchar(255) not null,
    VAR_TYPE_ nvarchar(255),
    REV_ int,
    TIME_ datetime not null,
    BYTEARRAY_ID_ nvarchar(64),
    DOUBLE_ double precision,
    LONG_ numeric(19,0),
    TEXT_ nvarchar(4000),
    TEXT2_ nvarchar(4000),
    primary key (ID_)
);

create table ACT_HI_COMMENT (
    ID_ nvarchar(64) not null,
    TYPE_ nvarchar(255),
    TIME_ datetime not null,
    USER_ID_ nvarchar(255),
    TASK_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    ACTION_ nvarchar(255),
    MESSAGE_ nvarchar(4000),
    FULL_MSG_ varbinary(max),
    primary key (ID_)
);

create table ACT_HI_ATTACHMENT (
    ID_ nvarchar(64) not null,
    REV_ integer,
    USER_ID_ nvarchar(255),
    NAME_ nvarchar(255),
    DESCRIPTION_ nvarchar(4000),
    TYPE_ nvarchar(255),
    TASK_ID_ nvarchar(64),
    PROC_INST_ID_ nvarchar(64),
    URL_ nvarchar(4000),
    CONTENT_ID_ nvarchar(64),
    TIME_ datetime,
    primary key (ID_)
);


create index ACT_IDX_HI_PRO_INST_END on ACT_HI_PROCINST(END_TIME_);
create index ACT_IDX_HI_PRO_I_BUSKEY on ACT_HI_PROCINST(BUSINESS_KEY_);
create index ACT_IDX_HI_PRO_SUPER_PROCINST on ACT_HI_PROCINST(SUPER_PROCESS_INSTANCE_ID_);
create index ACT_IDX_HI_ACT_INST_START on ACT_HI_ACTINST(START_TIME_);
create index ACT_IDX_HI_ACT_INST_END on ACT_HI_ACTINST(END_TIME_);
create index ACT_IDX_HI_DETAIL_PROC_INST on ACT_HI_DETAIL(PROC_INST_ID_);
create index ACT_IDX_HI_DETAIL_ACT_INST on ACT_HI_DETAIL(ACT_INST_ID_);
create index ACT_IDX_HI_DETAIL_TIME on ACT_HI_DETAIL(TIME_);
create index ACT_IDX_HI_DETAIL_NAME on ACT_HI_DETAIL(NAME_);
create index ACT_IDX_HI_DETAIL_TASK_ID on ACT_HI_DETAIL(TASK_ID_);
create index ACT_IDX_HI_PROCVAR_PROC_INST on ACT_HI_VARINST(PROC_INST_ID_);
create index ACT_IDX_HI_PROCVAR_TASK_ID on ACT_HI_VARINST(TASK_ID_);
create index ACT_IDX_HI_PROCVAR_EXE on ACT_HI_VARINST(EXECUTION_ID_);
create index ACT_IDX_HI_ACT_INST_PROCINST on ACT_HI_ACTINST(PROC_INST_ID_, ACT_ID_);
create index ACT_IDX_HI_ACT_INST_EXEC on ACT_HI_ACTINST(EXECUTION_ID_, ACT_ID_);
create index ACT_IDX_HI_IDENT_LNK_TASK on ACT_HI_IDENTITYLINK(TASK_ID_);
create index ACT_IDX_HI_IDENT_LNK_PROCINST on ACT_HI_IDENTITYLINK(PROC_INST_ID_);
create index ACT_IDX_HI_TASK_INST_PROCINST on ACT_HI_TASKINST(PROC_INST_ID_);
create table ACT_ID_PROPERTY (
    NAME_ nvarchar(64),
    VALUE_ nvarchar(300),
    REV_ int,
    primary key (NAME_)
);

insert into ACT_ID_PROPERTY
values ('schema.version', '6.8.0.0', 1);

create table ACT_ID_BYTEARRAY (
    ID_ nvarchar(64),
    REV_ int,
    NAME_ nvarchar(255),
    BYTES_  varbinary(max),
    primary key (ID_)
);

create table ACT_ID_GROUP (
    ID_ nvarchar(64),
    REV_ int,
    NAME_ nvarchar(255),
    TYPE_ nvarchar(255),
    primary key (ID_)
);

create table ACT_ID_MEMBERSHIP (
    USER_ID_ nvarchar(64),
    GROUP_ID_ nvarchar(64),
    primary key (USER_ID_, GROUP_ID_)
);

create table ACT_ID_USER (
    ID_ nvarchar(64),
    REV_ int,
    FIRST_ nvarchar(255),
    LAST_ nvarchar(255),
    DISPLAY_NAME_ nvarchar(255),
    EMAIL_ nvarchar(255),
    PWD_ nvarchar(255),
    PICTURE_ID_ nvarchar(64),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table ACT_ID_INFO (
    ID_ nvarchar(64),
    REV_ int,
    USER_ID_ nvarchar(64),
    TYPE_ nvarchar(64),
    KEY_ nvarchar(255),
    VALUE_ nvarchar(255),
    PASSWORD_ varbinary(max),
    PARENT_ID_ nvarchar(255),
    primary key (ID_)
);

create table ACT_ID_TOKEN (
    ID_ nvarchar(64) not null,
    REV_ int,
    TOKEN_VALUE_ nvarchar(255),
    TOKEN_DATE_ datetime,
    IP_ADDRESS_ nvarchar(255),
    USER_AGENT_ nvarchar(255),
    USER_ID_ nvarchar(255),
    TOKEN_DATA_ nvarchar(2000),
    primary key (ID_)
);

create table ACT_ID_PRIV (
    ID_ nvarchar(64) not null,
    NAME_ nvarchar(255) not null,
    primary key (ID_)
);

create table ACT_ID_PRIV_MAPPING (
    ID_ nvarchar(64) not null,
    PRIV_ID_ nvarchar(64) not null,
    USER_ID_ nvarchar(255),
    GROUP_ID_ nvarchar(255),
    primary key (ID_)
);

alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_GROUP
    foreign key (GROUP_ID_)
    references ACT_ID_GROUP;

alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_USER
    foreign key (USER_ID_)
    references ACT_ID_USER (ID_);

alter table ACT_ID_PRIV_MAPPING
    add constraint ACT_FK_PRIV_MAPPING
    foreign key (PRIV_ID_)
    references ACT_ID_PRIV (ID_);

create index ACT_IDX_PRIV_USER on ACT_ID_PRIV_MAPPING(USER_ID_);
create index ACT_IDX_PRIV_GROUP on ACT_ID_PRIV_MAPPING(GROUP_ID_);

alter table ACT_ID_PRIV
    add constraint ACT_UNIQ_PRIV_NAME
    unique (NAME_);


CREATE TABLE ACT_APP_DATABASECHANGELOGLOCK (ID int NOT NULL, LOCKED bit NOT NULL, LOCKGRANTED datetime2(3), LOCKEDBY nvarchar(255), CONSTRAINT PK_ACT_APP_DATABASECHANGELOGLOCK PRIMARY KEY (ID))

DELETE FROM ACT_APP_DATABASECHANGELOGLOCK

INSERT INTO ACT_APP_DATABASECHANGELOGLOCK (ID, LOCKED) VALUES (1, 0)

UPDATE ACT_APP_DATABASECHANGELOGLOCK SET LOCKED = 1, LOCKEDBY = '192.168.10.1 (192.168.10.1)', LOCKGRANTED = '2022-12-21T18:33:23.986' WHERE ID = 1 AND LOCKED = 0

CREATE TABLE ACT_APP_DATABASECHANGELOG (ID nvarchar(255) NOT NULL, AUTHOR nvarchar(255) NOT NULL, FILENAME nvarchar(255) NOT NULL, DATEEXECUTED datetime2(3) NOT NULL, ORDEREXECUTED int NOT NULL, EXECTYPE nvarchar(10) NOT NULL, MD5SUM nvarchar(35), DESCRIPTION nvarchar(255), COMMENTS nvarchar(255), TAG nvarchar(255), LIQUIBASE nvarchar(20), CONTEXTS nvarchar(255), LABELS nvarchar(255), DEPLOYMENT_ID nvarchar(10))

CREATE TABLE ACT_APP_DEPLOYMENT (ID_ varchar(255) NOT NULL, NAME_ varchar(255), CATEGORY_ varchar(255), KEY_ varchar(255), DEPLOY_TIME_ datetime, TENANT_ID_ varchar(255) CONSTRAINT DF_ACT_APP_DEPLOYMENT_TENANT_ID_ DEFAULT '', CONSTRAINT PK_ACT_APP_DEPLOYMENT PRIMARY KEY (ID_))

CREATE TABLE ACT_APP_DEPLOYMENT_RESOURCE (ID_ varchar(255) NOT NULL, NAME_ varchar(255), DEPLOYMENT_ID_ varchar(255), RESOURCE_BYTES_ varbinary(MAX), CONSTRAINT PK_APP_DEPLOYMENT_RESOURCE PRIMARY KEY (ID_))

ALTER TABLE ACT_APP_DEPLOYMENT_RESOURCE ADD CONSTRAINT ACT_FK_APP_RSRC_DPL FOREIGN KEY (DEPLOYMENT_ID_) REFERENCES ACT_APP_DEPLOYMENT (ID_)

CREATE NONCLUSTERED INDEX ACT_IDX_APP_RSRC_DPL ON ACT_APP_DEPLOYMENT_RESOURCE(DEPLOYMENT_ID_)

CREATE TABLE ACT_APP_APPDEF (ID_ varchar(255) NOT NULL, REV_ int NOT NULL, NAME_ varchar(255), KEY_ varchar(255) NOT NULL, VERSION_ int NOT NULL, CATEGORY_ varchar(255), DEPLOYMENT_ID_ varchar(255), RESOURCE_NAME_ varchar(4000), DESCRIPTION_ varchar(4000), TENANT_ID_ varchar(255) CONSTRAINT DF_ACT_APP_APPDEF_TENANT_ID_ DEFAULT '', CONSTRAINT PK_ACT_APP_APPDEF PRIMARY KEY (ID_))

ALTER TABLE ACT_APP_APPDEF ADD CONSTRAINT ACT_FK_APP_DEF_DPLY FOREIGN KEY (DEPLOYMENT_ID_) REFERENCES ACT_APP_DEPLOYMENT (ID_)

CREATE NONCLUSTERED INDEX ACT_IDX_APP_DEF_DPLY ON ACT_APP_APPDEF(DEPLOYMENT_ID_)

INSERT INTO ACT_APP_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('1', 'flowable', 'org/flowable/app/db/liquibase/flowable-app-db-changelog.xml', GETDATE(), 1, '8:496fc778bdf2ab13f2e1926d0e63e0a2', 'createTable tableName=ACT_APP_DEPLOYMENT; createTable tableName=ACT_APP_DEPLOYMENT_RESOURCE; addForeignKeyConstraint baseTableName=ACT_APP_DEPLOYMENT_RESOURCE, constraintName=ACT_FK_APP_RSRC_DPL, referencedTableName=ACT_APP_DEPLOYMENT; createIndex...', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004287')

CREATE UNIQUE NONCLUSTERED INDEX ACT_IDX_APP_DEF_UNIQ ON ACT_APP_APPDEF(KEY_, VERSION_, TENANT_ID_)

INSERT INTO ACT_APP_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('3', 'flowable', 'org/flowable/app/db/liquibase/flowable-app-db-changelog.xml', GETDATE(), 2, '8:f1f8aff320aade831944ebad24355f3d', 'createIndex indexName=ACT_IDX_APP_DEF_UNIQ, tableName=ACT_APP_APPDEF', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004287')

UPDATE ACT_APP_DATABASECHANGELOGLOCK SET LOCKED = 0, LOCKEDBY = NULL, LOCKGRANTED = NULL WHERE ID = 1



CREATE TABLE ACT_CMMN_DATABASECHANGELOGLOCK (ID int NOT NULL, LOCKED bit NOT NULL, LOCKGRANTED datetime2(3), LOCKEDBY nvarchar(255), CONSTRAINT PK_ACT_CMMN_DATABASECHANGELOGLOCK PRIMARY KEY (ID))

DELETE FROM ACT_CMMN_DATABASECHANGELOGLOCK

INSERT INTO ACT_CMMN_DATABASECHANGELOGLOCK (ID, LOCKED) VALUES (1, 0)

UPDATE ACT_CMMN_DATABASECHANGELOGLOCK SET LOCKED = 1, LOCKEDBY = '192.168.10.1 (192.168.10.1)', LOCKGRANTED = '2022-12-21T18:33:24.617' WHERE ID = 1 AND LOCKED = 0

CREATE TABLE ACT_CMMN_DATABASECHANGELOG (ID nvarchar(255) NOT NULL, AUTHOR nvarchar(255) NOT NULL, FILENAME nvarchar(255) NOT NULL, DATEEXECUTED datetime2(3) NOT NULL, ORDEREXECUTED int NOT NULL, EXECTYPE nvarchar(10) NOT NULL, MD5SUM nvarchar(35), DESCRIPTION nvarchar(255), COMMENTS nvarchar(255), TAG nvarchar(255), LIQUIBASE nvarchar(20), CONTEXTS nvarchar(255), LABELS nvarchar(255), DEPLOYMENT_ID nvarchar(10))

CREATE TABLE ACT_CMMN_DEPLOYMENT (ID_ varchar(255) NOT NULL, NAME_ varchar(255), CATEGORY_ varchar(255), KEY_ varchar(255), DEPLOY_TIME_ datetime, PARENT_DEPLOYMENT_ID_ varchar(255), TENANT_ID_ varchar(255) CONSTRAINT DF_ACT_CMMN_DEPLOYMENT_TENANT_ID_ DEFAULT '', CONSTRAINT PK_ACT_CMMN_DEPLOYMENT PRIMARY KEY (ID_))

CREATE TABLE ACT_CMMN_DEPLOYMENT_RESOURCE (ID_ varchar(255) NOT NULL, NAME_ varchar(255), DEPLOYMENT_ID_ varchar(255), RESOURCE_BYTES_ varbinary(MAX), CONSTRAINT PK_CMMN_DEPLOYMENT_RESOURCE PRIMARY KEY (ID_))

ALTER TABLE ACT_CMMN_DEPLOYMENT_RESOURCE ADD CONSTRAINT ACT_FK_CMMN_RSRC_DPL FOREIGN KEY (DEPLOYMENT_ID_) REFERENCES ACT_CMMN_DEPLOYMENT (ID_)

CREATE NONCLUSTERED INDEX ACT_IDX_CMMN_RSRC_DPL ON ACT_CMMN_DEPLOYMENT_RESOURCE(DEPLOYMENT_ID_)

CREATE TABLE ACT_CMMN_CASEDEF (ID_ varchar(255) NOT NULL, REV_ int NOT NULL, NAME_ varchar(255), KEY_ varchar(255) NOT NULL, VERSION_ int NOT NULL, CATEGORY_ varchar(255), DEPLOYMENT_ID_ varchar(255), RESOURCE_NAME_ varchar(4000), DESCRIPTION_ varchar(4000), HAS_GRAPHICAL_NOTATION_ bit, TENANT_ID_ varchar(255) CONSTRAINT DF_ACT_CMMN_CASEDEF_TENANT_ID_ DEFAULT '', CONSTRAINT PK_ACT_CMMN_CASEDEF PRIMARY KEY (ID_))

ALTER TABLE ACT_CMMN_CASEDEF ADD CONSTRAINT ACT_FK_CASE_DEF_DPLY FOREIGN KEY (DEPLOYMENT_ID_) REFERENCES ACT_CMMN_DEPLOYMENT (ID_)

CREATE NONCLUSTERED INDEX ACT_IDX_CASE_DEF_DPLY ON ACT_CMMN_CASEDEF(DEPLOYMENT_ID_)

CREATE TABLE ACT_CMMN_RU_CASE_INST (ID_ varchar(255) NOT NULL, REV_ int NOT NULL, BUSINESS_KEY_ varchar(255), NAME_ varchar(255), PARENT_ID_ varchar(255), CASE_DEF_ID_ varchar(255), STATE_ varchar(255), START_TIME_ datetime, START_USER_ID_ varchar(255), CALLBACK_ID_ varchar(255), CALLBACK_TYPE_ varchar(255), TENANT_ID_ varchar(255) CONSTRAINT DF_ACT_CMMN_RU_CASE_INST_TENANT_ID_ DEFAULT '', CONSTRAINT PK_ACT_CMMN_RU_CASE_INST PRIMARY KEY (ID_))

ALTER TABLE ACT_CMMN_RU_CASE_INST ADD CONSTRAINT ACT_FK_CASE_INST_CASE_DEF FOREIGN KEY (CASE_DEF_ID_) REFERENCES ACT_CMMN_CASEDEF (ID_)

CREATE NONCLUSTERED INDEX ACT_IDX_CASE_INST_CASE_DEF ON ACT_CMMN_RU_CASE_INST(CASE_DEF_ID_)

CREATE NONCLUSTERED INDEX ACT_IDX_CASE_INST_PARENT ON ACT_CMMN_RU_CASE_INST(PARENT_ID_)

CREATE TABLE ACT_CMMN_RU_PLAN_ITEM_INST (ID_ varchar(255) NOT NULL, REV_ int NOT NULL, CASE_DEF_ID_ varchar(255), CASE_INST_ID_ varchar(255), STAGE_INST_ID_ varchar(255), IS_STAGE_ bit, ELEMENT_ID_ varchar(255), NAME_ varchar(255), STATE_ varchar(255), START_TIME_ datetime, START_USER_ID_ varchar(255), REFERENCE_ID_ varchar(255), REFERENCE_TYPE_ varchar(255), TENANT_ID_ varchar(255) CONSTRAINT DF_ACT_CMMN_RU_PLAN_ITEM_INST_TENANT_ID_ DEFAULT '', CONSTRAINT PK_CMMN_PLAN_ITEM_INST PRIMARY KEY (ID_))

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD CONSTRAINT ACT_FK_PLAN_ITEM_CASE_DEF FOREIGN KEY (CASE_DEF_ID_) REFERENCES ACT_CMMN_CASEDEF (ID_)

CREATE NONCLUSTERED INDEX ACT_IDX_PLAN_ITEM_CASE_DEF ON ACT_CMMN_RU_PLAN_ITEM_INST(CASE_DEF_ID_)

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD CONSTRAINT ACT_FK_PLAN_ITEM_CASE_INST FOREIGN KEY (CASE_INST_ID_) REFERENCES ACT_CMMN_RU_CASE_INST (ID_)

CREATE NONCLUSTERED INDEX ACT_IDX_PLAN_ITEM_CASE_INST ON ACT_CMMN_RU_PLAN_ITEM_INST(CASE_INST_ID_)

CREATE TABLE ACT_CMMN_RU_SENTRY_PART_INST (ID_ varchar(255) NOT NULL, REV_ int NOT NULL, CASE_DEF_ID_ varchar(255), CASE_INST_ID_ varchar(255), PLAN_ITEM_INST_ID_ varchar(255), ON_PART_ID_ varchar(255), IF_PART_ID_ varchar(255), TIME_STAMP_ datetime, CONSTRAINT PK_CMMN_SENTRY_PART_INST PRIMARY KEY (ID_))

ALTER TABLE ACT_CMMN_RU_SENTRY_PART_INST ADD CONSTRAINT ACT_FK_SENTRY_CASE_DEF FOREIGN KEY (CASE_DEF_ID_) REFERENCES ACT_CMMN_CASEDEF (ID_)

CREATE NONCLUSTERED INDEX ACT_IDX_SENTRY_CASE_DEF ON ACT_CMMN_RU_SENTRY_PART_INST(CASE_DEF_ID_)

ALTER TABLE ACT_CMMN_RU_SENTRY_PART_INST ADD CONSTRAINT ACT_FK_SENTRY_CASE_INST FOREIGN KEY (CASE_INST_ID_) REFERENCES ACT_CMMN_RU_CASE_INST (ID_)

CREATE NONCLUSTERED INDEX ACT_IDX_SENTRY_CASE_INST ON ACT_CMMN_RU_SENTRY_PART_INST(CASE_INST_ID_)

ALTER TABLE ACT_CMMN_RU_SENTRY_PART_INST ADD CONSTRAINT ACT_FK_SENTRY_PLAN_ITEM FOREIGN KEY (PLAN_ITEM_INST_ID_) REFERENCES ACT_CMMN_RU_PLAN_ITEM_INST (ID_)

CREATE NONCLUSTERED INDEX ACT_IDX_SENTRY_PLAN_ITEM ON ACT_CMMN_RU_SENTRY_PART_INST(PLAN_ITEM_INST_ID_)

CREATE TABLE ACT_CMMN_RU_MIL_INST (ID_ varchar(255) NOT NULL, NAME_ varchar(255) NOT NULL, TIME_STAMP_ datetime NOT NULL, CASE_INST_ID_ varchar(255) NOT NULL, CASE_DEF_ID_ varchar(255) NOT NULL, ELEMENT_ID_ varchar(255) NOT NULL, CONSTRAINT PK_ACT_CMMN_RU_MIL_INST PRIMARY KEY (ID_))

ALTER TABLE ACT_CMMN_RU_MIL_INST ADD CONSTRAINT ACT_FK_MIL_CASE_DEF FOREIGN KEY (CASE_DEF_ID_) REFERENCES ACT_CMMN_CASEDEF (ID_)

CREATE NONCLUSTERED INDEX ACT_IDX_MIL_CASE_DEF ON ACT_CMMN_RU_MIL_INST(CASE_DEF_ID_)

ALTER TABLE ACT_CMMN_RU_MIL_INST ADD CONSTRAINT ACT_FK_MIL_CASE_INST FOREIGN KEY (CASE_INST_ID_) REFERENCES ACT_CMMN_RU_CASE_INST (ID_)

CREATE NONCLUSTERED INDEX ACT_IDX_MIL_CASE_INST ON ACT_CMMN_RU_MIL_INST(CASE_INST_ID_)

CREATE TABLE ACT_CMMN_HI_CASE_INST (ID_ varchar(255) NOT NULL, REV_ int NOT NULL, BUSINESS_KEY_ varchar(255), NAME_ varchar(255), PARENT_ID_ varchar(255), CASE_DEF_ID_ varchar(255), STATE_ varchar(255), START_TIME_ datetime, END_TIME_ datetime, START_USER_ID_ varchar(255), CALLBACK_ID_ varchar(255), CALLBACK_TYPE_ varchar(255), TENANT_ID_ varchar(255) CONSTRAINT DF_ACT_CMMN_HI_CASE_INST_TENANT_ID_ DEFAULT '', CONSTRAINT PK_ACT_CMMN_HI_CASE_INST PRIMARY KEY (ID_))

CREATE TABLE ACT_CMMN_HI_MIL_INST (ID_ varchar(255) NOT NULL, REV_ int NOT NULL, NAME_ varchar(255) NOT NULL, TIME_STAMP_ datetime NOT NULL, CASE_INST_ID_ varchar(255) NOT NULL, CASE_DEF_ID_ varchar(255) NOT NULL, ELEMENT_ID_ varchar(255) NOT NULL, CONSTRAINT PK_ACT_CMMN_HI_MIL_INST PRIMARY KEY (ID_))

INSERT INTO ACT_CMMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('1', 'flowable', 'org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml', GETDATE(), 1, '8:8b4b922d90b05ff27483abefc9597aa6', 'createTable tableName=ACT_CMMN_DEPLOYMENT; createTable tableName=ACT_CMMN_DEPLOYMENT_RESOURCE; addForeignKeyConstraint baseTableName=ACT_CMMN_DEPLOYMENT_RESOURCE, constraintName=ACT_FK_CMMN_RSRC_DPL, referencedTableName=ACT_CMMN_DEPLOYMENT; create...', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004724')

ALTER TABLE ACT_CMMN_CASEDEF ADD DGRM_RESOURCE_NAME_ varchar(4000)

ALTER TABLE ACT_CMMN_CASEDEF ADD HAS_START_FORM_KEY_ bit

ALTER TABLE ACT_CMMN_DEPLOYMENT_RESOURCE ADD GENERATED_ bit

ALTER TABLE ACT_CMMN_RU_CASE_INST ADD LOCK_TIME_ datetime

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD ITEM_DEFINITION_ID_ varchar(255)

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD ITEM_DEFINITION_TYPE_ varchar(255)

INSERT INTO ACT_CMMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('2', 'flowable', 'org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml', GETDATE(), 2, '8:65e39b3d385706bb261cbeffe7533cbe', 'addColumn tableName=ACT_CMMN_CASEDEF; addColumn tableName=ACT_CMMN_DEPLOYMENT_RESOURCE; addColumn tableName=ACT_CMMN_RU_CASE_INST; addColumn tableName=ACT_CMMN_RU_PLAN_ITEM_INST', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004724')

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD IS_COMPLETEABLE_ bit

ALTER TABLE ACT_CMMN_RU_CASE_INST ADD IS_COMPLETEABLE_ bit

CREATE NONCLUSTERED INDEX ACT_IDX_PLAN_ITEM_STAGE_INST ON ACT_CMMN_RU_PLAN_ITEM_INST(STAGE_INST_ID_)

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD IS_COUNT_ENABLED_ bit

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD VAR_COUNT_ int

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD SENTRY_PART_INST_COUNT_ int

INSERT INTO ACT_CMMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('3', 'flowable', 'org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml', GETDATE(), 3, '8:c01f6e802b49436b4489040da3012359', 'addColumn tableName=ACT_CMMN_RU_PLAN_ITEM_INST; addColumn tableName=ACT_CMMN_RU_CASE_INST; createIndex indexName=ACT_IDX_PLAN_ITEM_STAGE_INST, tableName=ACT_CMMN_RU_PLAN_ITEM_INST; addColumn tableName=ACT_CMMN_RU_PLAN_ITEM_INST; addColumn tableNam...', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004724')

CREATE TABLE ACT_CMMN_HI_PLAN_ITEM_INST (ID_ varchar(255) NOT NULL, REV_ int NOT NULL, NAME_ varchar(255), STATE_ varchar(255), CASE_DEF_ID_ varchar(255), CASE_INST_ID_ varchar(255), STAGE_INST_ID_ varchar(255), IS_STAGE_ bit, ELEMENT_ID_ varchar(255), ITEM_DEFINITION_ID_ varchar(255), ITEM_DEFINITION_TYPE_ varchar(255), CREATED_TIME_ datetime, LAST_AVAILABLE_TIME_ datetime, LAST_ENABLED_TIME_ datetime, LAST_DISABLED_TIME_ datetime, LAST_STARTED_TIME_ datetime, LAST_SUSPENDED_TIME_ datetime, COMPLETED_TIME_ datetime, OCCURRED_TIME_ datetime, TERMINATED_TIME_ datetime, EXIT_TIME_ datetime, ENDED_TIME_ datetime, LAST_UPDATED_TIME_ datetime, START_USER_ID_ varchar(255), REFERENCE_ID_ varchar(255), REFERENCE_TYPE_ varchar(255), TENANT_ID_ varchar(255) CONSTRAINT DF_ACT_CMMN_HI_PLAN_ITEM_INST_TENANT_ID_ DEFAULT '', CONSTRAINT PK_ACT_CMMN_HI_PLAN_ITEM_INST PRIMARY KEY (ID_))

ALTER TABLE ACT_CMMN_RU_MIL_INST ADD TENANT_ID_ varchar(255) CONSTRAINT DF_ACT_CMMN_RU_MIL_INST_TENANT_ID_ DEFAULT ''

ALTER TABLE ACT_CMMN_HI_MIL_INST ADD TENANT_ID_ varchar(255) CONSTRAINT DF_ACT_CMMN_HI_MIL_INST_TENANT_ID_ DEFAULT ''

INSERT INTO ACT_CMMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('4', 'flowable', 'org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml', GETDATE(), 4, '8:e40d29cb79345b7fb5afd38a7f0ba8fc', 'createTable tableName=ACT_CMMN_HI_PLAN_ITEM_INST; addColumn tableName=ACT_CMMN_RU_MIL_INST; addColumn tableName=ACT_CMMN_HI_MIL_INST', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004724')

CREATE UNIQUE NONCLUSTERED INDEX ACT_IDX_CASE_DEF_UNIQ ON ACT_CMMN_CASEDEF(KEY_, VERSION_, TENANT_ID_)

INSERT INTO ACT_CMMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('6', 'flowable', 'org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml', GETDATE(), 5, '8:10e82e26a7fee94c32a92099c059c18c', 'createIndex indexName=ACT_IDX_CASE_DEF_UNIQ, tableName=ACT_CMMN_CASEDEF', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004724')

exec sp_rename 'ACT_CMMN_RU_PLAN_ITEM_INST.START_TIME_', 'CREATE_TIME_'

exec sp_rename 'ACT_CMMN_HI_PLAN_ITEM_INST.CREATED_TIME_', 'CREATE_TIME_'

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD LAST_AVAILABLE_TIME_ datetime

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD LAST_ENABLED_TIME_ datetime

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD LAST_DISABLED_TIME_ datetime

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD LAST_STARTED_TIME_ datetime

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD LAST_SUSPENDED_TIME_ datetime

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD COMPLETED_TIME_ datetime

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD OCCURRED_TIME_ datetime

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD TERMINATED_TIME_ datetime

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD EXIT_TIME_ datetime

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD ENDED_TIME_ datetime

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD ENTRY_CRITERION_ID_ varchar(255)

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD EXIT_CRITERION_ID_ varchar(255)

ALTER TABLE ACT_CMMN_HI_PLAN_ITEM_INST ADD ENTRY_CRITERION_ID_ varchar(255)

ALTER TABLE ACT_CMMN_HI_PLAN_ITEM_INST ADD EXIT_CRITERION_ID_ varchar(255)

INSERT INTO ACT_CMMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('7', 'flowable', 'org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml', GETDATE(), 6, '8:530bc81a1e30618ccf4a2da1f7c6c043', 'renameColumn newColumnName=CREATE_TIME_, oldColumnName=START_TIME_, tableName=ACT_CMMN_RU_PLAN_ITEM_INST; renameColumn newColumnName=CREATE_TIME_, oldColumnName=CREATED_TIME_, tableName=ACT_CMMN_HI_PLAN_ITEM_INST; addColumn tableName=ACT_CMMN_RU_P...', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004724')

ALTER TABLE ACT_CMMN_HI_PLAN_ITEM_INST ADD SHOW_IN_OVERVIEW_ bit

INSERT INTO ACT_CMMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('8', 'flowable', 'org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml', GETDATE(), 7, '8:e8c2eb1ce28bc301efe07e0e29757781', 'addColumn tableName=ACT_CMMN_HI_PLAN_ITEM_INST', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004724')

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD EXTRA_VALUE_ varchar(255)

ALTER TABLE ACT_CMMN_HI_PLAN_ITEM_INST ADD EXTRA_VALUE_ varchar(255)

INSERT INTO ACT_CMMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('9', 'flowable', 'org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml', GETDATE(), 8, '8:4cb4782b9bdec5ced2a64c525aa7b3a0', 'addColumn tableName=ACT_CMMN_RU_PLAN_ITEM_INST; addColumn tableName=ACT_CMMN_HI_PLAN_ITEM_INST', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004724')

ALTER TABLE ACT_CMMN_RU_CASE_INST ADD REFERENCE_ID_ varchar(255)

ALTER TABLE ACT_CMMN_RU_CASE_INST ADD REFERENCE_TYPE_ varchar(255)

CREATE NONCLUSTERED INDEX ACT_IDX_CASE_INST_REF_ID_ ON ACT_CMMN_RU_CASE_INST(REFERENCE_ID_)

ALTER TABLE ACT_CMMN_HI_CASE_INST ADD REFERENCE_ID_ varchar(255)

ALTER TABLE ACT_CMMN_HI_CASE_INST ADD REFERENCE_TYPE_ varchar(255)

INSERT INTO ACT_CMMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('10', 'flowable', 'org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml', GETDATE(), 9, '8:341c16be247f5d17badc9809da8691f9', 'addColumn tableName=ACT_CMMN_RU_CASE_INST; addColumn tableName=ACT_CMMN_RU_CASE_INST; createIndex indexName=ACT_IDX_CASE_INST_REF_ID_, tableName=ACT_CMMN_RU_CASE_INST; addColumn tableName=ACT_CMMN_HI_CASE_INST; addColumn tableName=ACT_CMMN_HI_CASE...', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004724')

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD DERIVED_CASE_DEF_ID_ varchar(255)

ALTER TABLE ACT_CMMN_HI_PLAN_ITEM_INST ADD DERIVED_CASE_DEF_ID_ varchar(255)

INSERT INTO ACT_CMMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('11', 'flowable', 'org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml', GETDATE(), 10, '8:d7c4da9276bcfffbfb0ebfb25e3f7b05', 'addColumn tableName=ACT_CMMN_RU_PLAN_ITEM_INST; addColumn tableName=ACT_CMMN_HI_PLAN_ITEM_INST', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004724')

ALTER TABLE ACT_CMMN_RU_CASE_INST ADD LOCK_OWNER_ varchar(255)

INSERT INTO ACT_CMMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('12', 'flowable', 'org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml', GETDATE(), 11, '8:adf4ecc45f2aa9a44a5626b02e1d6f98', 'addColumn tableName=ACT_CMMN_RU_CASE_INST', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004724')

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST ADD LAST_UNAVAILABLE_TIME_ datetime

ALTER TABLE ACT_CMMN_HI_PLAN_ITEM_INST ADD LAST_UNAVAILABLE_TIME_ datetime

INSERT INTO ACT_CMMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('13', 'flowable', 'org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml', GETDATE(), 12, '8:7550626f964ab5518464709408333ec1', 'addColumn tableName=ACT_CMMN_RU_PLAN_ITEM_INST; addColumn tableName=ACT_CMMN_HI_PLAN_ITEM_INST', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004724')

ALTER TABLE ACT_CMMN_RU_CASE_INST ADD LAST_REACTIVATION_TIME_ datetime

ALTER TABLE ACT_CMMN_RU_CASE_INST ADD LAST_REACTIVATION_USER_ID_ varchar(255)

ALTER TABLE ACT_CMMN_HI_CASE_INST ADD LAST_REACTIVATION_TIME_ datetime

ALTER TABLE ACT_CMMN_HI_CASE_INST ADD LAST_REACTIVATION_USER_ID_ varchar(255)

INSERT INTO ACT_CMMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('14', 'flowable', 'org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml', GETDATE(), 13, '8:086b40b3a05596dcc8a8d7479922d494', 'addColumn tableName=ACT_CMMN_RU_CASE_INST; addColumn tableName=ACT_CMMN_HI_CASE_INST', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004724')

ALTER TABLE ACT_CMMN_RU_CASE_INST ADD BUSINESS_STATUS_ varchar(255)

ALTER TABLE ACT_CMMN_HI_CASE_INST ADD BUSINESS_STATUS_ varchar(255)

INSERT INTO ACT_CMMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('16', 'flowable', 'org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml', GETDATE(), 14, '8:a697a222ddd99dd15b36516a252f1c63', 'addColumn tableName=ACT_CMMN_RU_CASE_INST; addColumn tableName=ACT_CMMN_HI_CASE_INST', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004724')

CREATE NONCLUSTERED INDEX ACT_IDX_HI_CASE_INST_END ON ACT_CMMN_HI_CASE_INST(END_TIME_)

INSERT INTO ACT_CMMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('17', 'flowable', 'org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml', GETDATE(), 15, '8:d3706c5813a9b97fd2a59d12a9523946', 'createIndex indexName=ACT_IDX_HI_CASE_INST_END, tableName=ACT_CMMN_HI_CASE_INST', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644004724')

UPDATE ACT_CMMN_DATABASECHANGELOGLOCK SET LOCKED = 0, LOCKEDBY = NULL, LOCKGRANTED = NULL WHERE ID = 1



CREATE TABLE FLW_EV_DATABASECHANGELOGLOCK (ID int NOT NULL, LOCKED bit NOT NULL, LOCKGRANTED datetime2(3), LOCKEDBY nvarchar(255), CONSTRAINT PK_FLW_EV_DATABASECHANGELOGLOCK PRIMARY KEY (ID))

DELETE FROM FLW_EV_DATABASECHANGELOGLOCK

INSERT INTO FLW_EV_DATABASECHANGELOGLOCK (ID, LOCKED) VALUES (1, 0)

UPDATE FLW_EV_DATABASECHANGELOGLOCK SET LOCKED = 1, LOCKEDBY = '192.168.10.1 (192.168.10.1)', LOCKGRANTED = '2022-12-21T18:33:25.574' WHERE ID = 1 AND LOCKED = 0

CREATE TABLE FLW_EV_DATABASECHANGELOG (ID nvarchar(255) NOT NULL, AUTHOR nvarchar(255) NOT NULL, FILENAME nvarchar(255) NOT NULL, DATEEXECUTED datetime2(3) NOT NULL, ORDEREXECUTED int NOT NULL, EXECTYPE nvarchar(10) NOT NULL, MD5SUM nvarchar(35), DESCRIPTION nvarchar(255), COMMENTS nvarchar(255), TAG nvarchar(255), LIQUIBASE nvarchar(20), CONTEXTS nvarchar(255), LABELS nvarchar(255), DEPLOYMENT_ID nvarchar(10))

CREATE TABLE FLW_EVENT_DEPLOYMENT (ID_ varchar(255) NOT NULL, NAME_ varchar(255), CATEGORY_ varchar(255), DEPLOY_TIME_ datetime, TENANT_ID_ varchar(255), PARENT_DEPLOYMENT_ID_ varchar(255), CONSTRAINT PK_FLW_EVENT_DEPLOYMENT PRIMARY KEY (ID_))

CREATE TABLE FLW_EVENT_RESOURCE (ID_ varchar(255) NOT NULL, NAME_ varchar(255), DEPLOYMENT_ID_ varchar(255), RESOURCE_BYTES_ varbinary(MAX), CONSTRAINT PK_FLW_EVENT_RESOURCE PRIMARY KEY (ID_))

CREATE TABLE FLW_EVENT_DEFINITION (ID_ varchar(255) NOT NULL, NAME_ varchar(255), VERSION_ int, KEY_ varchar(255), CATEGORY_ varchar(255), DEPLOYMENT_ID_ varchar(255), TENANT_ID_ varchar(255), RESOURCE_NAME_ varchar(255), DESCRIPTION_ varchar(255), CONSTRAINT PK_FLW_EVENT_DEFINITION PRIMARY KEY (ID_))

CREATE UNIQUE NONCLUSTERED INDEX ACT_IDX_EVENT_DEF_UNIQ ON FLW_EVENT_DEFINITION(KEY_, VERSION_, TENANT_ID_)

CREATE TABLE FLW_CHANNEL_DEFINITION (ID_ varchar(255) NOT NULL, NAME_ varchar(255), VERSION_ int, KEY_ varchar(255), CATEGORY_ varchar(255), DEPLOYMENT_ID_ varchar(255), CREATE_TIME_ datetime, TENANT_ID_ varchar(255), RESOURCE_NAME_ varchar(255), DESCRIPTION_ varchar(255), CONSTRAINT PK_FLW_CHANNEL_DEFINITION PRIMARY KEY (ID_))

CREATE UNIQUE NONCLUSTERED INDEX ACT_IDX_CHANNEL_DEF_UNIQ ON FLW_CHANNEL_DEFINITION(KEY_, VERSION_, TENANT_ID_)

INSERT INTO FLW_EV_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('1', 'flowable', 'org/flowable/eventregistry/db/liquibase/flowable-eventregistry-db-changelog.xml', GETDATE(), 1, '8:1b0c48c9cf7945be799d868a2626d687', 'createTable tableName=FLW_EVENT_DEPLOYMENT; createTable tableName=FLW_EVENT_RESOURCE; createTable tableName=FLW_EVENT_DEFINITION; createIndex indexName=ACT_IDX_EVENT_DEF_UNIQ, tableName=FLW_EVENT_DEFINITION; createTable tableName=FLW_CHANNEL_DEFIN...', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644005653')

ALTER TABLE FLW_CHANNEL_DEFINITION ADD TYPE_ varchar(255)

ALTER TABLE FLW_CHANNEL_DEFINITION ADD IMPLEMENTATION_ varchar(255)

INSERT INTO FLW_EV_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('2', 'flowable', 'org/flowable/eventregistry/db/liquibase/flowable-eventregistry-db-changelog.xml', GETDATE(), 2, '8:0ea825feb8e470558f0b5754352b9cda', 'addColumn tableName=FLW_CHANNEL_DEFINITION; addColumn tableName=FLW_CHANNEL_DEFINITION', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644005653')

INSERT INTO FLW_EV_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('3', 'flowable', 'org/flowable/eventregistry/db/liquibase/flowable-eventregistry-db-changelog.xml', GETDATE(), 3, '8:3c2bb293350b5cbe6504331980c9dcee', 'customChange', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644005653')

UPDATE FLW_EV_DATABASECHANGELOGLOCK SET LOCKED = 0, LOCKEDBY = NULL, LOCKGRANTED = NULL WHERE ID = 1



CREATE TABLE ACT_DMN_DATABASECHANGELOGLOCK (ID int NOT NULL, LOCKED bit NOT NULL, LOCKGRANTED datetime2(3), LOCKEDBY nvarchar(255), CONSTRAINT PK_ACT_DMN_DATABASECHANGELOGLOCK PRIMARY KEY (ID))

DELETE FROM ACT_DMN_DATABASECHANGELOGLOCK

INSERT INTO ACT_DMN_DATABASECHANGELOGLOCK (ID, LOCKED) VALUES (1, 0)

UPDATE ACT_DMN_DATABASECHANGELOGLOCK SET LOCKED = 1, LOCKEDBY = '192.168.10.1 (192.168.10.1)', LOCKGRANTED = '2022-12-21T18:33:25.879' WHERE ID = 1 AND LOCKED = 0

CREATE TABLE ACT_DMN_DATABASECHANGELOG (ID nvarchar(255) NOT NULL, AUTHOR nvarchar(255) NOT NULL, FILENAME nvarchar(255) NOT NULL, DATEEXECUTED datetime2(3) NOT NULL, ORDEREXECUTED int NOT NULL, EXECTYPE nvarchar(10) NOT NULL, MD5SUM nvarchar(35), DESCRIPTION nvarchar(255), COMMENTS nvarchar(255), TAG nvarchar(255), LIQUIBASE nvarchar(20), CONTEXTS nvarchar(255), LABELS nvarchar(255), DEPLOYMENT_ID nvarchar(10))

CREATE TABLE ACT_DMN_DEPLOYMENT (ID_ varchar(255) NOT NULL, NAME_ varchar(255), CATEGORY_ varchar(255), DEPLOY_TIME_ datetime, TENANT_ID_ varchar(255), PARENT_DEPLOYMENT_ID_ varchar(255), CONSTRAINT PK_ACT_DMN_DEPLOYMENT PRIMARY KEY (ID_))

CREATE TABLE ACT_DMN_DEPLOYMENT_RESOURCE (ID_ varchar(255) NOT NULL, NAME_ varchar(255), DEPLOYMENT_ID_ varchar(255), RESOURCE_BYTES_ varbinary(MAX), CONSTRAINT PK_ACT_DMN_DEPLOYMENT_RESOURCE PRIMARY KEY (ID_))

CREATE TABLE ACT_DMN_DECISION_TABLE (ID_ varchar(255) NOT NULL, NAME_ varchar(255), VERSION_ int, KEY_ varchar(255), CATEGORY_ varchar(255), DEPLOYMENT_ID_ varchar(255), PARENT_DEPLOYMENT_ID_ varchar(255), TENANT_ID_ varchar(255), RESOURCE_NAME_ varchar(255), DESCRIPTION_ varchar(255), CONSTRAINT PK_ACT_DMN_DECISION_TABLE PRIMARY KEY (ID_))

INSERT INTO ACT_DMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('1', 'activiti', 'org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml', GETDATE(), 1, '8:c8701f1c71018b55029f450b2e9a10a1', 'createTable tableName=ACT_DMN_DEPLOYMENT; createTable tableName=ACT_DMN_DEPLOYMENT_RESOURCE; createTable tableName=ACT_DMN_DECISION_TABLE', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644005973')

CREATE TABLE ACT_DMN_HI_DECISION_EXECUTION (ID_ varchar(255) NOT NULL, DECISION_DEFINITION_ID_ varchar(255), DEPLOYMENT_ID_ varchar(255), START_TIME_ datetime, END_TIME_ datetime, INSTANCE_ID_ varchar(255), EXECUTION_ID_ varchar(255), ACTIVITY_ID_ varchar(255), FAILED_ bit CONSTRAINT DF_ACT_DMN_HI_DECISION_EXECUTION_FAILED_ DEFAULT 0, TENANT_ID_ varchar(255), EXECUTION_JSON_ varchar(MAX), CONSTRAINT PK_ACT_DMN_HI_DECISION_EXECUTION PRIMARY KEY (ID_))

INSERT INTO ACT_DMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('2', 'flowable', 'org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml', GETDATE(), 2, '8:47f94b27feb7df8a30d4e338c7bd5fb8', 'createTable tableName=ACT_DMN_HI_DECISION_EXECUTION', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644005973')

ALTER TABLE ACT_DMN_HI_DECISION_EXECUTION ADD SCOPE_TYPE_ varchar(255)

INSERT INTO ACT_DMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('3', 'flowable', 'org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml', GETDATE(), 3, '8:ac17eae89fbdccb6e08daf3c7797b579', 'addColumn tableName=ACT_DMN_HI_DECISION_EXECUTION', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644005973')

DECLARE @sql [nvarchar](MAX)
SELECT @sql = N'ALTER TABLE ACT_DMN_DECISION_TABLE DROP CONSTRAINT ' + QUOTENAME([df].[name]) FROM [sys].[columns] AS [c] INNER JOIN [sys].[default_constraints] AS [df] ON [df].[object_id] = [c].[default_object_id] WHERE [c].[object_id] = OBJECT_ID(N'ACT_DMN_DECISION_TABLE') AND [c].[name] = N'PARENT_DEPLOYMENT_ID_'
EXEC sp_executesql @sql

ALTER TABLE ACT_DMN_DECISION_TABLE DROP COLUMN PARENT_DEPLOYMENT_ID_

INSERT INTO ACT_DMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('4', 'flowable', 'org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml', GETDATE(), 4, '8:f73aabc4529e7292c2942073d1cff6f9', 'dropColumn columnName=PARENT_DEPLOYMENT_ID_, tableName=ACT_DMN_DECISION_TABLE', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644005973')

CREATE UNIQUE NONCLUSTERED INDEX ACT_IDX_DEC_TBL_UNIQ ON ACT_DMN_DECISION_TABLE(KEY_, VERSION_, TENANT_ID_)

INSERT INTO ACT_DMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('6', 'flowable', 'org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml', GETDATE(), 5, '8:646c6a061e0b6e8a62e69844ff96abb0', 'createIndex indexName=ACT_IDX_DEC_TBL_UNIQ, tableName=ACT_DMN_DECISION_TABLE', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644005973')

DROP INDEX ACT_IDX_DEC_TBL_UNIQ ON ACT_DMN_DECISION_TABLE

exec sp_rename 'ACT_DMN_DECISION_TABLE', 'ACT_DMN_DECISION'

CREATE UNIQUE NONCLUSTERED INDEX ACT_IDX_DMN_DEC_UNIQ ON ACT_DMN_DECISION(KEY_, VERSION_, TENANT_ID_)

INSERT INTO ACT_DMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('7', 'flowable', 'org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml', GETDATE(), 6, '8:215a499ff7ae77685b55355245b8b708', 'dropIndex indexName=ACT_IDX_DEC_TBL_UNIQ, tableName=ACT_DMN_DECISION_TABLE; renameTable newTableName=ACT_DMN_DECISION, oldTableName=ACT_DMN_DECISION_TABLE; createIndex indexName=ACT_IDX_DMN_DEC_UNIQ, tableName=ACT_DMN_DECISION', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644005973')

ALTER TABLE ACT_DMN_DECISION ADD DECISION_TYPE_ varchar(255)

INSERT INTO ACT_DMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('8', 'flowable', 'org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml', GETDATE(), 7, '8:5355bee389318afed91a11702f2df032', 'addColumn tableName=ACT_DMN_DECISION', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644005973')

CREATE NONCLUSTERED INDEX ACT_IDX_DMN_INSTANCE_ID ON ACT_DMN_HI_DECISION_EXECUTION(INSTANCE_ID_)

INSERT INTO ACT_DMN_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('9', 'flowable', 'org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml', GETDATE(), 8, '8:0fe82086431b1953d293f0199f805876', 'createIndex indexName=ACT_IDX_DMN_INSTANCE_ID, tableName=ACT_DMN_HI_DECISION_EXECUTION', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644005973')

UPDATE ACT_DMN_DATABASECHANGELOGLOCK SET LOCKED = 0, LOCKEDBY = NULL, LOCKGRANTED = NULL WHERE ID = 1



CREATE TABLE ACT_FO_DATABASECHANGELOGLOCK (ID int NOT NULL, LOCKED bit NOT NULL, LOCKGRANTED datetime2(3), LOCKEDBY nvarchar(255), CONSTRAINT PK_ACT_FO_DATABASECHANGELOGLOCK PRIMARY KEY (ID))

DELETE FROM ACT_FO_DATABASECHANGELOGLOCK

INSERT INTO ACT_FO_DATABASECHANGELOGLOCK (ID, LOCKED) VALUES (1, 0)

UPDATE ACT_FO_DATABASECHANGELOGLOCK SET LOCKED = 1, LOCKEDBY = '192.168.10.1 (192.168.10.1)', LOCKGRANTED = '2022-12-21T18:33:26.344' WHERE ID = 1 AND LOCKED = 0

CREATE TABLE ACT_FO_DATABASECHANGELOG (ID nvarchar(255) NOT NULL, AUTHOR nvarchar(255) NOT NULL, FILENAME nvarchar(255) NOT NULL, DATEEXECUTED datetime2(3) NOT NULL, ORDEREXECUTED int NOT NULL, EXECTYPE nvarchar(10) NOT NULL, MD5SUM nvarchar(35), DESCRIPTION nvarchar(255), COMMENTS nvarchar(255), TAG nvarchar(255), LIQUIBASE nvarchar(20), CONTEXTS nvarchar(255), LABELS nvarchar(255), DEPLOYMENT_ID nvarchar(10))

CREATE TABLE ACT_FO_FORM_DEPLOYMENT (ID_ varchar(255) NOT NULL, NAME_ varchar(255), CATEGORY_ varchar(255), DEPLOY_TIME_ datetime, TENANT_ID_ varchar(255), PARENT_DEPLOYMENT_ID_ varchar(255), CONSTRAINT PK_ACT_FO_FORM_DEPLOYMENT PRIMARY KEY (ID_))

CREATE TABLE ACT_FO_FORM_RESOURCE (ID_ varchar(255) NOT NULL, NAME_ varchar(255), DEPLOYMENT_ID_ varchar(255), RESOURCE_BYTES_ varbinary(MAX), CONSTRAINT PK_ACT_FO_FORM_RESOURCE PRIMARY KEY (ID_))

CREATE TABLE ACT_FO_FORM_DEFINITION (ID_ varchar(255) NOT NULL, NAME_ varchar(255), VERSION_ int, KEY_ varchar(255), CATEGORY_ varchar(255), DEPLOYMENT_ID_ varchar(255), PARENT_DEPLOYMENT_ID_ varchar(255), TENANT_ID_ varchar(255), RESOURCE_NAME_ varchar(255), DESCRIPTION_ varchar(255), CONSTRAINT PK_ACT_FO_FORM_DEFINITION PRIMARY KEY (ID_))

CREATE TABLE ACT_FO_FORM_INSTANCE (ID_ varchar(255) NOT NULL, FORM_DEFINITION_ID_ varchar(255) NOT NULL, TASK_ID_ varchar(255), PROC_INST_ID_ varchar(255), PROC_DEF_ID_ varchar(255), SUBMITTED_DATE_ datetime, SUBMITTED_BY_ varchar(255), FORM_VALUES_ID_ varchar(255), TENANT_ID_ varchar(255), CONSTRAINT PK_ACT_FO_FORM_INSTANCE PRIMARY KEY (ID_))

INSERT INTO ACT_FO_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('1', 'activiti', 'org/flowable/form/db/liquibase/flowable-form-db-changelog.xml', GETDATE(), 1, '8:033ebf9380889aed7c453927ecc3250d', 'createTable tableName=ACT_FO_FORM_DEPLOYMENT; createTable tableName=ACT_FO_FORM_RESOURCE; createTable tableName=ACT_FO_FORM_DEFINITION; createTable tableName=ACT_FO_FORM_INSTANCE', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644006420')

ALTER TABLE ACT_FO_FORM_INSTANCE ADD SCOPE_ID_ varchar(255)

ALTER TABLE ACT_FO_FORM_INSTANCE ADD SCOPE_TYPE_ varchar(255)

ALTER TABLE ACT_FO_FORM_INSTANCE ADD SCOPE_DEFINITION_ID_ varchar(255)

INSERT INTO ACT_FO_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('2', 'flowable', 'org/flowable/form/db/liquibase/flowable-form-db-changelog.xml', GETDATE(), 2, '8:986365ceb40445ce3b27a8e6b40f159b', 'addColumn tableName=ACT_FO_FORM_INSTANCE', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644006420')

DECLARE @sql [nvarchar](MAX)
SELECT @sql = N'ALTER TABLE ACT_FO_FORM_DEFINITION DROP CONSTRAINT ' + QUOTENAME([df].[name]) FROM [sys].[columns] AS [c] INNER JOIN [sys].[default_constraints] AS [df] ON [df].[object_id] = [c].[default_object_id] WHERE [c].[object_id] = OBJECT_ID(N'ACT_FO_FORM_DEFINITION') AND [c].[name] = N'PARENT_DEPLOYMENT_ID_'
EXEC sp_executesql @sql

ALTER TABLE ACT_FO_FORM_DEFINITION DROP COLUMN PARENT_DEPLOYMENT_ID_

INSERT INTO ACT_FO_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('3', 'flowable', 'org/flowable/form/db/liquibase/flowable-form-db-changelog.xml', GETDATE(), 3, '8:abf482518ceb09830ef674e52c06bf15', 'dropColumn columnName=PARENT_DEPLOYMENT_ID_, tableName=ACT_FO_FORM_DEFINITION', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644006420')

CREATE UNIQUE NONCLUSTERED INDEX ACT_IDX_FORM_DEF_UNIQ ON ACT_FO_FORM_DEFINITION(KEY_, VERSION_, TENANT_ID_)

INSERT INTO ACT_FO_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('5', 'flowable', 'org/flowable/form/db/liquibase/flowable-form-db-changelog.xml', GETDATE(), 4, '8:b4be732b89e5ca028bdd520c6ad4d446', 'createIndex indexName=ACT_IDX_FORM_DEF_UNIQ, tableName=ACT_FO_FORM_DEFINITION', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644006420')

CREATE NONCLUSTERED INDEX ACT_IDX_FORM_TASK ON ACT_FO_FORM_INSTANCE(TASK_ID_)

CREATE NONCLUSTERED INDEX ACT_IDX_FORM_PROC ON ACT_FO_FORM_INSTANCE(PROC_INST_ID_)

CREATE NONCLUSTERED INDEX ACT_IDX_FORM_SCOPE ON ACT_FO_FORM_INSTANCE(SCOPE_ID_, SCOPE_TYPE_)

INSERT INTO ACT_FO_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('6', 'flowable', 'org/flowable/form/db/liquibase/flowable-form-db-changelog.xml', GETDATE(), 5, '8:384bbd364a649b67c3ca1bcb72fe537f', 'createIndex indexName=ACT_IDX_FORM_TASK, tableName=ACT_FO_FORM_INSTANCE; createIndex indexName=ACT_IDX_FORM_PROC, tableName=ACT_FO_FORM_INSTANCE; createIndex indexName=ACT_IDX_FORM_SCOPE, tableName=ACT_FO_FORM_INSTANCE', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644006420')

UPDATE ACT_FO_DATABASECHANGELOGLOCK SET LOCKED = 0, LOCKEDBY = NULL, LOCKGRANTED = NULL WHERE ID = 1



CREATE TABLE ACT_CO_DATABASECHANGELOGLOCK (ID int NOT NULL, LOCKED bit NOT NULL, LOCKGRANTED datetime2(3), LOCKEDBY nvarchar(255), CONSTRAINT PK_ACT_CO_DATABASECHANGELOGLOCK PRIMARY KEY (ID))

DELETE FROM ACT_CO_DATABASECHANGELOGLOCK

INSERT INTO ACT_CO_DATABASECHANGELOGLOCK (ID, LOCKED) VALUES (1, 0)

UPDATE ACT_CO_DATABASECHANGELOGLOCK SET LOCKED = 1, LOCKEDBY = '192.168.10.1 (192.168.10.1)', LOCKGRANTED = '2022-12-21T18:33:26.673' WHERE ID = 1 AND LOCKED = 0

CREATE TABLE ACT_CO_DATABASECHANGELOG (ID nvarchar(255) NOT NULL, AUTHOR nvarchar(255) NOT NULL, FILENAME nvarchar(255) NOT NULL, DATEEXECUTED datetime2(3) NOT NULL, ORDEREXECUTED int NOT NULL, EXECTYPE nvarchar(10) NOT NULL, MD5SUM nvarchar(35), DESCRIPTION nvarchar(255), COMMENTS nvarchar(255), TAG nvarchar(255), LIQUIBASE nvarchar(20), CONTEXTS nvarchar(255), LABELS nvarchar(255), DEPLOYMENT_ID nvarchar(10))

CREATE TABLE ACT_CO_CONTENT_ITEM (ID_ varchar(255) NOT NULL, NAME_ varchar(255) NOT NULL, MIME_TYPE_ varchar(255), TASK_ID_ varchar(255), PROC_INST_ID_ varchar(255), CONTENT_STORE_ID_ varchar(255), CONTENT_STORE_NAME_ varchar(255), FIELD_ varchar(400), CONTENT_AVAILABLE_ bit CONSTRAINT DF_ACT_CO_CONTENT_ITEM_CONTENT_AVAILABLE_ DEFAULT 0, CREATED_ datetime, CREATED_BY_ varchar(255), LAST_MODIFIED_ datetime, LAST_MODIFIED_BY_ varchar(255), CONTENT_SIZE_ bigint CONSTRAINT DF_ACT_CO_CONTENT_ITEM_CONTENT_SIZE_ DEFAULT 0, TENANT_ID_ varchar(255), CONSTRAINT PK_ACT_CO_CONTENT_ITEM PRIMARY KEY (ID_))

CREATE NONCLUSTERED INDEX idx_contitem_taskid ON ACT_CO_CONTENT_ITEM(TASK_ID_)

CREATE NONCLUSTERED INDEX idx_contitem_procid ON ACT_CO_CONTENT_ITEM(PROC_INST_ID_)

INSERT INTO ACT_CO_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('1', 'activiti', 'org/flowable/content/db/liquibase/flowable-content-db-changelog.xml', GETDATE(), 1, '8:7644d7165cfe799200a2abdd3419e8b6', 'createTable tableName=ACT_CO_CONTENT_ITEM; createIndex indexName=idx_contitem_taskid, tableName=ACT_CO_CONTENT_ITEM; createIndex indexName=idx_contitem_procid, tableName=ACT_CO_CONTENT_ITEM', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644006759')

ALTER TABLE ACT_CO_CONTENT_ITEM ADD SCOPE_ID_ varchar(255)

ALTER TABLE ACT_CO_CONTENT_ITEM ADD SCOPE_TYPE_ varchar(255)

CREATE NONCLUSTERED INDEX idx_contitem_scope ON ACT_CO_CONTENT_ITEM(SCOPE_ID_, SCOPE_TYPE_)

INSERT INTO ACT_CO_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('2', 'flowable', 'org/flowable/content/db/liquibase/flowable-content-db-changelog.xml', GETDATE(), 2, '8:fe7b11ac7dbbf9c43006b23bbab60bab', 'addColumn tableName=ACT_CO_CONTENT_ITEM; createIndex indexName=idx_contitem_scope, tableName=ACT_CO_CONTENT_ITEM', '', 'EXECUTED', NULL, NULL, '4.3.5', '1644006759')

UPDATE ACT_CO_DATABASECHANGELOGLOCK SET LOCKED = 0, LOCKEDBY = NULL, LOCKGRANTED = NULL WHERE ID = 1

