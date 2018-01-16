create table ACT_RU_TASK (
    ID_ varchar(64) not null,
    REV_ int null,
    EXECUTION_ID_ varchar(64) null,
    PROC_INST_ID_ varchar(64) null,
    PROC_DEF_ID_ varchar(64) null,
    SCOPE_ID_ varchar(255) null,
    SUB_SCOPE_ID_ varchar(255) null,
    SCOPE_TYPE_ varchar(255) null,
    SCOPE_DEFINITION_ID_ varchar(255) null,
    NAME_ varchar(255) null,
    PARENT_TASK_ID_ varchar(64) null,
    DESCRIPTION_ varchar(255) null,
    TASK_DEF_KEY_ varchar(255) null,
    OWNER_ varchar(255) null,
    ASSIGNEE_ varchar(255) null,
    DELEGATION_ varchar(64) null,
    PRIORITY_ int null,
    CREATE_TIME_ datetime null,
    DUE_DATE_ datetime null,
    CATEGORY_ varchar(255) null,
    SUSPENSION_STATE_ int null,
    TENANT_ID_ varchar(255) default '' null,
    FORM_KEY_ varchar(255) null,
    CLAIM_TIME_ datetime null,
    IS_COUNT_ENABLED_ tinyint null,
    VAR_COUNT_ int null, 
    ID_LINK_COUNT_ int null,
    primary key (ID_)
);

create index ACT_IDX_TASK_CREATE on ACT_RU_TASK(CREATE_TIME_);
create index ACT_IDX_TASK_SCOPE on ACT_RU_TASK(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TASK_SUB_SCOPE on ACT_RU_TASK(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TASK_SCOPE_DEF on ACT_RU_TASK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('task.schema.version', '6.3.0.0', 1);