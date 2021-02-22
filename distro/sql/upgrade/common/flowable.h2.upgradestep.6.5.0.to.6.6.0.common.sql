update ACT_GE_PROPERTY set VALUE_ = '6.6.0.0' where NAME_ = 'common.schema.version';

alter table ACT_RU_ENTITYLINK add column ROOT_SCOPE_ID_ varchar(255);
alter table ACT_RU_ENTITYLINK add column ROOT_SCOPE_TYPE_ varchar(255);
create index ACT_IDX_ENT_LNK_ROOT_SCOPE on ACT_RU_ENTITYLINK(ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_);

alter table ACT_HI_ENTITYLINK add column ROOT_SCOPE_ID_ varchar(255);
alter table ACT_HI_ENTITYLINK add column ROOT_SCOPE_TYPE_ varchar(255);
create index ACT_IDX_HI_ENT_LNK_ROOT_SCOPE on ACT_HI_ENTITYLINK(ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_);

alter table ACT_RU_ENTITYLINK add column SUB_SCOPE_ID_ varchar(255);
alter table ACT_RU_ENTITYLINK add column PARENT_ELEMENT_ID_ varchar(255);

alter table ACT_HI_ENTITYLINK add column SUB_SCOPE_ID_ varchar(255);
alter table ACT_HI_ENTITYLINK add column PARENT_ELEMENT_ID_ varchar(255);

update ACT_GE_PROPERTY set VALUE_ = '6.6.0.0' where NAME_ = 'entitylink.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.6.0.0' where NAME_ = 'identitylink.schema.version';

alter table ACT_RU_JOB add column CATEGORY_ varchar(255);

alter table ACT_RU_TIMER_JOB add column CATEGORY_ varchar(255);

alter table ACT_RU_SUSPENDED_JOB add column CATEGORY_ varchar(255);

alter table ACT_RU_DEADLETTER_JOB add column CATEGORY_ varchar(255);

create table ACT_RU_EXTERNAL_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    CATEGORY_ varchar(255),
    TYPE_ varchar(255) NOT NULL,
    LOCK_EXP_TIME_ timestamp,
    LOCK_OWNER_ varchar(255),
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    ELEMENT_ID_ varchar(255),
    ELEMENT_NAME_ varchar(255),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    CUSTOM_VALUES_ID_ varchar(64),
    CREATE_TIME_ timestamp,
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
);

create index ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID on ACT_RU_EXTERNAL_JOB(EXCEPTION_STACK_ID_);
create index ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID on ACT_RU_EXTERNAL_JOB(CUSTOM_VALUES_ID_);

alter table ACT_RU_EXTERNAL_JOB
    add constraint ACT_FK_EXTERNAL_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);

alter table ACT_RU_EXTERNAL_JOB
    add constraint ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES
    foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_EJOB_SCOPE on ACT_RU_EXTERNAL_JOB(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_EJOB_SUB_SCOPE on ACT_RU_EXTERNAL_JOB(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_EJOB_SCOPE_DEF on ACT_RU_EXTERNAL_JOB(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

alter table ACT_RU_JOB add column CORRELATION_ID_ varchar(255);
alter table ACT_RU_TIMER_JOB add column CORRELATION_ID_ varchar(255);
alter table ACT_RU_SUSPENDED_JOB add column CORRELATION_ID_ varchar(255);
alter table ACT_RU_DEADLETTER_JOB add column CORRELATION_ID_ varchar(255);
alter table ACT_RU_EXTERNAL_JOB add column CORRELATION_ID_ varchar(255);

create index ACT_IDX_JOB_CORRELATION_ID on ACT_RU_JOB(CORRELATION_ID_);
create index ACT_IDX_TIMER_JOB_CORRELATION_ID on ACT_RU_TIMER_JOB(CORRELATION_ID_);
create index ACT_IDX_SUSPENDED_JOB_CORRELATION_ID on ACT_RU_SUSPENDED_JOB(CORRELATION_ID_);
create index ACT_IDX_DEADLETTER_JOB_CORRELATION_ID on ACT_RU_DEADLETTER_JOB(CORRELATION_ID_);
create index ACT_IDX_EXTERNAL_JOB_CORRELATION_ID on ACT_RU_EXTERNAL_JOB(CORRELATION_ID_);

update ACT_GE_PROPERTY set VALUE_ = '6.6.0.0' where NAME_ = 'job.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.6.0.0' where NAME_ = 'batch.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.6.0.0' where NAME_ = 'task.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.6.0.0' where NAME_ = 'variable.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.6.0.0' where NAME_ = 'eventsubscription.schema.version';
