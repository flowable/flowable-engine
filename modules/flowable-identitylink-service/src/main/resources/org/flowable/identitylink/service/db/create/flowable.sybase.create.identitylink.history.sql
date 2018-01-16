create table ACT_HI_IDENTITYLINK (
    ID_ varchar(64) not null,
    GROUP_ID_ varchar(255) null,
    TYPE_ varchar(255) null,
    USER_ID_ varchar(255) null,
    TASK_ID_ varchar(64) null,
    CREATE_TIME_ datetime null,
    PROC_INST_ID_ varchar(64) null,
    primary key (ID_)
);

create index ACT_IDX_HI_IDENT_LNK_USER on ACT_HI_IDENTITYLINK(USER_ID_);