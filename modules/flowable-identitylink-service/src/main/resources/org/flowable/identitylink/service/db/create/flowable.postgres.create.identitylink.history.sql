create table ACT_HI_IDENTITYLINK (
    ID_ varchar(64),
    GROUP_ID_ varchar(191),
    TYPE_ varchar(191),
    USER_ID_ varchar(191),
    TASK_ID_ varchar(64),
    CREATE_TIME_ timestamp,
    PROC_INST_ID_ varchar(64),
    SCOPE_ID_ varchar(191),
    SCOPE_TYPE_ varchar(191),
    SCOPE_DEFINITION_ID_ varchar(191),
    primary key (ID_)
);

create index ACT_IDX_HI_IDENT_LNK_USER on ACT_HI_IDENTITYLINK(USER_ID_);
create index ACT_IDX_HI_IDENT_LNK_SCOPE on ACT_HI_IDENTITYLINK(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_IDENT_LNK_SCOPE_DEF on ACT_HI_IDENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);
