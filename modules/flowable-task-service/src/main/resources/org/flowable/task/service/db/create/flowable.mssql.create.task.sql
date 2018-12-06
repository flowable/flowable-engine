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

create table ACT_HI_TSK_LOG (
    LOG_NR_              numeric(19,0) IDENTITY (1,1),
    TYPE_                nvarchar(64),
    TASK_ID_             nvarchar(64) not null,
    TIME_STAMP_          datetime not null,
    USER_ID_             nvarchar(255),
    DATA_                nvarchar(4000),
    EXECUTION_ID_        nvarchar(64),
    PROC_INST_ID_        nvarchar(64),
    PROC_DEF_ID_         nvarchar(64),
    SCOPE_ID_            nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    SUB_SCOPE_ID_        nvarchar(255),
    SCOPE_TYPE_          nvarchar(255),
    TENANT_ID_           nvarchar(255) default ''
);

create index ACT_IDX_TASK_CREATE on ACT_RU_TASK(CREATE_TIME_);
create index ACT_IDX_TASK_SCOPE on ACT_RU_TASK(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TASK_SUB_SCOPE on ACT_RU_TASK(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TASK_SCOPE_DEF on ACT_RU_TASK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_HI_TASK_LOG_NUMBER on ACT_HI_TSK_LOG(LOG_NR_);

insert into ACT_GE_PROPERTY values ('task.schema.version', '6.4.1.3', 1);