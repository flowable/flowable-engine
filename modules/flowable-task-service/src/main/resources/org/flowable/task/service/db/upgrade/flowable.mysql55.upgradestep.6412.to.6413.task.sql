create table ACT_HI_TSK_LOG (
  ID_              bigint auto_increment,
  TYPE_                varchar(64),
  TASK_ID_             varchar(64) not null,
  TIME_STAMP_          datetime    not null,
  USER_ID_             varchar(255),
  DATA_                varchar(4000),
  EXECUTION_ID_        varchar(64),
  PROC_INST_ID_        varchar(64),
  PROC_DEF_ID_         varchar(64),
  SCOPE_ID_            varchar(255),
  SCOPE_DEFINITION_ID_ varchar(255),
  SUB_SCOPE_ID_        varchar(255),
  SCOPE_TYPE_          varchar(255),
  TENANT_ID_           varchar(255) default ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

create index ACT_IDX_HI_TASK_LOG_NUMBER on ACT_HI_TSK_LOG(ID_);

update ACT_GE_PROPERTY set VALUE_ = '6.4.1.3' where NAME_ = 'task.schema.version';
