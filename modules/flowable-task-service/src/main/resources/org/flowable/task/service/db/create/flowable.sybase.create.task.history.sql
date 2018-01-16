create table ACT_HI_TASKINST (
    ID_ varchar(64) not null,
    REV_ int default 1  null,
    PROC_DEF_ID_ varchar(64) null,
    TASK_DEF_KEY_ varchar(255) null,
    PROC_INST_ID_ varchar(64) null,
    EXECUTION_ID_ varchar(64) null,
    SCOPE_ID_ varchar(255) null,
    SUB_SCOPE_ID_ varchar(255) null,
    SCOPE_TYPE_ varchar(255) null,
    SCOPE_DEFINITION_ID_ varchar(255) null,
    NAME_ varchar(255) null,
    PARENT_TASK_ID_ varchar(64) null,
    DESCRIPTION_ varchar(355) null,
    OWNER_ varchar(255) null,
    ASSIGNEE_ varchar(255) null,
    START_TIME_ datetime not null,
    CLAIM_TIME_ datetime null,
    END_TIME_ datetime null,
    DURATION_ numeric(19,0) null,
    DELETE_REASON_ varchar(4000) null,
    PRIORITY_ int null,
    DUE_DATE_ datetime null,
    FORM_KEY_ varchar(255) null,
    CATEGORY_ varchar(255) null,
    TENANT_ID_ varchar(255) default '' null,
    LAST_UPDATED_TIME_ datetime null,
    primary key (ID_)
);

create index ACT_IDX_HI_TASK_SCOPE on ACT_HI_TASKINST(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_TASK_SUB_SCOPE on ACT_HI_TASKINST(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_TASK_SCOPE_DEF on ACT_HI_TASKINST(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);