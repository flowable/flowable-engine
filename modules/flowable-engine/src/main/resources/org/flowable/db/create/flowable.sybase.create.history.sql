create table ACT_HI_PROCINST (
    ID_ varchar(64) not null,
    REV_ int default 1,
    PROC_INST_ID_ varchar(64) not null,
    BUSINESS_KEY_ varchar(255) null,
    PROC_DEF_ID_ varchar(64) not null,
    START_TIME_ datetime not null,
    END_TIME_ datetime null,
    DURATION_ numeric(19,0) null,
    START_USER_ID_ varchar(255) null,
    START_ACT_ID_ varchar(255) null,
    END_ACT_ID_ varchar(255) null,
    SUPER_PROCESS_INSTANCE_ID_ varchar(64) null,
    DELETE_REASON_ varchar(4000) null,
    TENANT_ID_ varchar(255) default '' null,
    NAME_ varchar(255) null,
    CALLBACK_ID_ varchar(255) null,
    CALLBACK_TYPE_ varchar(255) null,
    primary key (ID_),
    unique (PROC_INST_ID_)
);

create table ACT_HI_ACTINST (
    ID_ varchar(64) not null,
    REV_ int default 1 null,
    PROC_DEF_ID_ varchar(64) not null,
    PROC_INST_ID_ varchar(64) not null,
    EXECUTION_ID_ varchar(64) not null,
    ACT_ID_ varchar(255) not null,
    TASK_ID_ varchar(64) null,
    CALL_PROC_INST_ID_ varchar(64) null,
    ACT_NAME_ varchar(255) null,
    ACT_TYPE_ varchar(255) not null,
    ASSIGNEE_ varchar(255) null,
    START_TIME_ datetime not null,
    END_TIME_ datetime null,
    DURATION_ numeric(19,0) null,
    DELETE_REASON_ varchar(4000) null,
    TENANT_ID_ varchar(255) default '' null,
    primary key (ID_)
);

create table ACT_HI_DETAIL (
    ID_ varchar(64) not null,
    TYPE_ varchar(255) not null,
    PROC_INST_ID_ varchar(64) null,
    EXECUTION_ID_ varchar(64) null,
    TASK_ID_ varchar(64) null,
    ACT_INST_ID_ varchar(64) null,
    NAME_ varchar(255) not null,
    VAR_TYPE_ varchar(255) null,
    REV_ int null,
    TIME_ datetime not null,
    BYTEARRAY_ID_ varchar(64) null,
    DOUBLE_ double precision null,
    LONG_ numeric(19,0) null,
    TEXT_ varchar(4000) null,
    TEXT2_ varchar(4000) null,
    primary key (ID_)
);

create table ACT_HI_COMMENT (
    ID_ varchar(64) not null,
    TYPE_ varchar(255) null,
    TIME_ datetime not null,
    USER_ID_ varchar(255) null,
    TASK_ID_ varchar(64) null,
    PROC_INST_ID_ varchar(64) null,
    ACTION_ varchar(255) null,
    MESSAGE_ varchar(4000) null,
    FULL_MSG_ IMAGE null,
    primary key (ID_)
);

create table ACT_HI_ATTACHMENT (
    ID_ varchar(64) not null,
    REV_ int null,
    USER_ID_ varchar(255) null,
    NAME_ varchar(255) null,
    DESCRIPTION_ varchar(4000) null,
    TYPE_ varchar(255) null,
    TASK_ID_ varchar(64) null,
    PROC_INST_ID_ varchar(64) null,
    URL_ varchar(4000) null,
    CONTENT_ID_ varchar(64) null,
    TIME_ datetime null,
    primary key (ID_)
);


create index ACT_IDX_HI_PRO_INST_END on ACT_HI_PROCINST(END_TIME_);
create index ACT_IDX_HI_PRO_I_BUSKEY on ACT_HI_PROCINST(BUSINESS_KEY_);
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
