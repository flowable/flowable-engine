create table ACT_HI_TSK_LOG (
  ID_ numeric(19,0) IDENTITY (1,1),
  TYPE_ nvarchar(64),
  TASK_ID_ nvarchar(64) not null,
  TIME_STAMP_ datetime not null,
  USER_ID_ nvarchar(255),
  DATA_ nvarchar(4000),
  EXECUTION_ID_ nvarchar(64),
  PROC_INST_ID_ nvarchar(64),
  PROC_DEF_ID_ nvarchar(64),
  SCOPE_ID_ nvarchar(255),
  SCOPE_DEFINITION_ID_ nvarchar(255),
  SUB_SCOPE_ID_ nvarchar(255),
  SCOPE_TYPE_ nvarchar(255),
  TENANT_ID_ nvarchar(255) default '',
  primary key (ID_)
);

create table ACT_RU_ENTITYLINK (
    ID_ nvarchar(64),
    REV_ int,
    CREATE_TIME_ datetime,
    LINK_TYPE_ nvarchar(255),
    SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    REF_SCOPE_ID_ nvarchar(255),
    REF_SCOPE_TYPE_ nvarchar(255),
    REF_SCOPE_DEFINITION_ID_ nvarchar(255),
    HIERARCHY_TYPE_ nvarchar(255),
    primary key (ID_)
);

create index ACT_IDX_ENT_LNK_SCOPE on ACT_RU_ENTITYLINK(SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_ENT_LNK_SCOPE_DEF on ACT_RU_ENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);

create table ACT_HI_ENTITYLINK (
    ID_ nvarchar(64),
    LINK_TYPE_ nvarchar(255),
    CREATE_TIME_ datetime,
    SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    REF_SCOPE_ID_ nvarchar(255),
    REF_SCOPE_TYPE_ nvarchar(255),
    REF_SCOPE_DEFINITION_ID_ nvarchar(255),
    HIERARCHY_TYPE_ nvarchar(255),
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
