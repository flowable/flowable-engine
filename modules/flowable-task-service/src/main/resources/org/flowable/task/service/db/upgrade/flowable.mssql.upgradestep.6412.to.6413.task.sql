create table FLW_TSK_LOG (
  LOG_NR_       numeric(19,0) IDENTITY (1,1),
  TYPE_         nvarchar(64),
  TASK_ID_      nvarchar(64) not null,
  TIME_STAMP_   datetime not null,
  USER_ID_      nvarchar(255),
  DATA_         nvarchar(4000),
  EXECUTION_ID_ nvarchar(64),
  PROC_INST_ID_ nvarchar(64),
  SCOPE_ID_     nvarchar(255),
  SUB_SCOPE_ID_ nvarchar(255),
  SCOPE_TYPE_   nvarchar(255),
  TENANT_ID_    nvarchar(255) default ''
);

create index FLW_IDX_TASK_LOG_NUMBER on FLW_TSK_LOG(LOG_NR_);

update ACT_GE_PROPERTY set VALUE_ = '6.4.1.3' where NAME_ = 'task.schema.version';
