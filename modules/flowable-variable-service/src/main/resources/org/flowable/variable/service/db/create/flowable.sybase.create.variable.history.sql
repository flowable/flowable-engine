create table ACT_HI_VARINST (
    ID_ varchar(64) not null,
    REV_ int default 1 null,
    PROC_INST_ID_ varchar(64) null,
    EXECUTION_ID_ varchar(64) null,
    TASK_ID_ varchar(64) null,
    NAME_ varchar(255) not null,
    VAR_TYPE_ varchar(100) null,
    SCOPE_ID_ varchar(255) null,
    SUB_SCOPE_ID_ varchar(255) null,
    SCOPE_TYPE_ varchar(255) null,
    BYTEARRAY_ID_ varchar(64) null,
    DOUBLE_ double precision null,
    LONG_ numeric(19,0) null,
    TEXT_ varchar(4000) null,
    TEXT2_ varchar(4000) null,
    CREATE_TIME_ datetime null,
    LAST_UPDATED_TIME_ datetime null,
    primary key (ID_)
);

create index ACT_IDX_HI_PROCVAR_NAME_TYPE on ACT_HI_VARINST(NAME_, VAR_TYPE_);
create index ACT_IDX_HI_VAR_SCOPE_ID_TYPE on ACT_HI_VARINST(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_VAR_SUB_ID_TYPE on ACT_HI_VARINST(SUB_SCOPE_ID_, SCOPE_TYPE_);
