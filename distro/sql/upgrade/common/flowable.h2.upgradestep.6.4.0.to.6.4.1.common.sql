create table ACT_HI_TSK_LOG (
  ID_ identity,
  TYPE_ varchar(64),
  TASK_ID_ varchar(64) not null,
  TIME_STAMP_ timestamp not null,
  USER_ID_ varchar(255),
  DATA_ varchar(4000),
  EXECUTION_ID_ varchar(64),
  PROC_INST_ID_ varchar(64),
  PROC_DEF_ID_ varchar(64),
  SCOPE_ID_ varchar(255),
  SCOPE_DEFINITION_ID_ varchar(255),
  SUB_SCOPE_ID_ varchar(255),
  SCOPE_TYPE_ varchar(255),
  TENANT_ID_ varchar(255) default '',
  primary key (ID_)
);

create table ACT_RU_ENTITYLINK (
    ID_ varchar(64),
    REV_ integer,
    CREATE_TIME_ timestamp,
    LINK_TYPE_ varchar(255),
    SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    REF_SCOPE_ID_ varchar(255),
    REF_SCOPE_TYPE_ varchar(255),
    REF_SCOPE_DEFINITION_ID_ varchar(255),
    HIERARCHY_TYPE_ varchar(255),
    primary key (ID_)
);

create index ACT_IDX_ENT_LNK_SCOPE on ACT_RU_ENTITYLINK(SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_ENT_LNK_SCOPE_DEF on ACT_RU_ENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);

create table ACT_HI_ENTITYLINK (
    ID_ varchar(64),
    LINK_TYPE_ varchar(255),
    CREATE_TIME_ timestamp,
    SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    REF_SCOPE_ID_ varchar(255),
    REF_SCOPE_TYPE_ varchar(255),
    REF_SCOPE_DEFINITION_ID_ varchar(255),
    HIERARCHY_TYPE_ varchar(255),
    primary key (ID_)
);

create index ACT_IDX_HI_ENT_LNK_SCOPE on ACT_HI_ENTITYLINK(SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_HI_ENT_LNK_SCOPE_DEF on ACT_HI_ENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);

update ACT_GE_PROPERTY set VALUE_ = '6.4.1.3' where NAME_ = 'common.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.4.1.3' where NAME_ = 'task.schema.version';

insert into ACT_GE_PROPERTY values ('entitylink.schema.version', '6.4.1.3', 1);

update ACT_GE_PROPERTY set VALUE_ = '6.4.1.3' where NAME_ = 'job.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.4.1.3' where NAME_ = 'identitylink.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.4.1.3' where NAME_ = 'variable.schema.version';
