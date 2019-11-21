create table ACT_RU_ACTINST (
    ID_ NVARCHAR2(64) not null,
    REV_ INTEGER default 1,
    PROC_DEF_ID_ NVARCHAR2(64) not null,
    PROC_INST_ID_ NVARCHAR2(64) not null,
    EXECUTION_ID_ NVARCHAR2(64) not null,
    ACT_ID_ NVARCHAR2(255) not null,
    TASK_ID_ NVARCHAR2(64),
    CALL_PROC_INST_ID_ NVARCHAR2(64),
    ACT_NAME_ NVARCHAR2(255),
    ACT_TYPE_ NVARCHAR2(255) not null,
    ASSIGNEE_ NVARCHAR2(255),
    START_TIME_ TIMESTAMP(6) not null,
    END_TIME_ TIMESTAMP(6),
    DURATION_ NUMBER(19,0),
    DELETE_REASON_ NVARCHAR2(2000),
    TENANT_ID_ NVARCHAR2(255) default '',
    primary key (ID_)
);

create index ACT_IDX_RU_ACTI_START on ACT_RU_ACTINST(START_TIME_);
create index ACT_IDX_RU_ACTI_END on ACT_RU_ACTINST(END_TIME_);
create index ACT_IDX_RU_ACTI_PROC on ACT_RU_ACTINST(PROC_INST_ID_);
create index ACT_IDX_RU_ACTI_PROC_ACT on ACT_RU_ACTINST(PROC_INST_ID_, ACT_ID_);
create index ACT_IDX_RU_ACTI_EXEC on ACT_RU_ACTINST(EXECUTION_ID_);
create index ACT_IDX_RU_ACTI_EXEC_ACT on ACT_RU_ACTINST(EXECUTION_ID_, ACT_ID_);

delete
from ACT_RU_VARIABLE
where TYPE_ in ('item', 'message')
  and BYTEARRAY_ID_ is null
  and DOUBLE_ is null
  and LONG_ is null
  and TEXT_ is null
  and TEXT2_ is null;

delete
from ACT_RU_VARIABLE
where TYPE_ = 'null'
  and NAME_ in ('org.activiti.engine.impl.bpmn.CURRENT_MESSAGE', 'org.flowable.engine.impl.bpmn.CURRENT_MESSAGE');

update ACT_GE_PROPERTY set VALUE_ = '6.4.1.3' where NAME_ = 'schema.version';

delete
from ACT_HI_VARINST
where VAR_TYPE_ in ('item', 'message')
  and BYTEARRAY_ID_ is null
  and DOUBLE_ is null
  and LONG_ is null
  and TEXT_ is null
  and TEXT2_ is null;

delete from ACT_HI_VARINST
where VAR_TYPE_ = 'null'
and NAME_ in ('org.activiti.engine.impl.bpmn.CURRENT_MESSAGE', 'org.flowable.engine.impl.bpmn.CURRENT_MESSAGE');
