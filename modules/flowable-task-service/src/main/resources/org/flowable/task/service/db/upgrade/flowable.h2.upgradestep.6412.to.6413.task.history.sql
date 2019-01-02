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