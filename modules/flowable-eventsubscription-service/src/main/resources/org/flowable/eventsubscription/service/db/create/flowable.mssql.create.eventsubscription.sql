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
