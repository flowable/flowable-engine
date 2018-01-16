create table ACT_RU_IDENTITYLINK (
    ID_ varchar(64) not null,
    REV_ int null,
    GROUP_ID_ varchar(255) null,
    TYPE_ varchar(255) null,
    USER_ID_ varchar(255) null,
    TASK_ID_ varchar(64) null,
    PROC_INST_ID_ varchar(64) null,
    PROC_DEF_ID_ varchar(64) null,
    primary key (ID_)
);

create index ACT_IDX_IDENT_LNK_USER on ACT_RU_IDENTITYLINK(USER_ID_);
create index ACT_IDX_IDENT_LNK_GROUP on ACT_RU_IDENTITYLINK(GROUP_ID_);

insert into ACT_GE_PROPERTY values ('identitylink.schema.version', '6.3.0.0', 1);