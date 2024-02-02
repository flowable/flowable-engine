create table ACT_RU_ACTINST (
  ID_ varchar(64) not null,
  REV_ integer default 1,
  PROC_DEF_ID_ varchar(64) not null,
  PROC_INST_ID_ varchar(64) not null,
  EXECUTION_ID_ varchar(64) not null,
  ACT_ID_ varchar(255) not null,
  TASK_ID_ varchar(64),
  CALL_PROC_INST_ID_ varchar(64),
  ACT_NAME_ varchar(255),
  ACT_TYPE_ varchar(255) not null,
  ASSIGNEE_ varchar(255),
  START_TIME_ timestamp not null,
  END_TIME_ timestamp,
  DURATION_ bigint,
  DELETE_REASON_ varchar(4000),
  TENANT_ID_ varchar(255) default '',
  primary key (ID_)
);

create index ACT_IDX_RU_ACTI_START on ACT_RU_ACTINST(START_TIME_);
create index ACT_IDX_RU_ACTI_END on ACT_RU_ACTINST(END_TIME_);
create index ACT_IDX_RU_ACTI_PROC on ACT_RU_ACTINST(PROC_INST_ID_);
create index ACT_IDX_RU_ACTI_PROC_ACT on ACT_RU_ACTINST(PROC_INST_ID_, ACT_ID_);
create index ACT_IDX_RU_ACTI_EXEC on ACT_RU_ACTINST(EXECUTION_ID_);
create index ACT_IDX_RU_ACTI_EXEC_ACT on ACT_RU_ACTINST(EXECUTION_ID_, ACT_ID_);

update ACT_GE_PROPERTY set VALUE_ = '6.4.1.2' where NAME_ = 'schema.version';
