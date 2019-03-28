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
