create table FLW_TSK_LOG (
  LOG_NR_       bigint      not null GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  TYPE_         varchar(64),
  TASK_ID_      varchar(64) not null,
  TIME_STAMP_   timestamp   not null,
  USER_ID_      varchar(255),
  DATA_         varchar(4000),
  EXECUTION_ID_ varchar(64),
  PROC_INST_ID_ varchar(64),
  SCOPE_ID_     varchar(255),
  SUB_SCOPE_ID_ varchar(255),
  SCOPE_TYPE_   varchar(255),
  TENANT_ID_    varchar(255) default ''
);

create index FLW_IDX_TASK_LOG_NUMBER on FLW_TSK_LOG(LOG_NR_);

update ACT_GE_PROPERTY set VALUE_ = '6.4.1.3' where NAME_ = 'task.schema.version';
