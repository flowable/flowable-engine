insert into ACT_GE_PROPERTY values ('common.schema.version', '6.2.0.0', 1);
insert into ACT_GE_PROPERTY values ('identitylink.schema.version', '6.2.0.0', 1);
alter table ACT_RU_TASK add SCOPE_ID_ nvarchar(255);
alter table ACT_RU_TASK add SUB_SCOPE_ID_ nvarchar(255);
alter table ACT_RU_TASK add SCOPE_TYPE_ nvarchar(255);
alter table ACT_RU_TASK add SCOPE_DEFINITION_ID_ nvarchar(255);

create index ACT_IDX_TASK_SCOPE on ACT_RU_TASK(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TASK_SUB_SCOPE on ACT_RU_TASK(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TASK_SCOPE_DEF on ACT_RU_TASK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('task.schema.version', '6.2.0.0', 1);
alter table ACT_RU_VARIABLE add SCOPE_ID_ nvarchar(255);
alter table ACT_RU_VARIABLE add SUB_SCOPE_ID_ nvarchar(255);
alter table ACT_RU_VARIABLE add SCOPE_TYPE_ nvarchar(255);

create index ACT_IDX_RU_VAR_SCOPE_ID_TYPE on ACT_RU_VARIABLE(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_RU_VAR_SUB_ID_TYPE on ACT_RU_VARIABLE(SUB_SCOPE_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('variable.schema.version', '6.2.0.0', 1);
insert into ACT_GE_PROPERTY values ('job.schema.version', '6.2.0.0', 1);

alter table ACT_HI_TASKINST add SCOPE_ID_ nvarchar(255);
alter table ACT_HI_TASKINST add SUB_SCOPE_ID_ nvarchar(255);
alter table ACT_HI_TASKINST add SCOPE_TYPE_ nvarchar(255);
alter table ACT_HI_TASKINST add SCOPE_DEFINITION_ID_ nvarchar(255);

create index ACT_IDX_HI_TASK_SCOPE on ACT_HI_TASKINST(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_TASK_SUB_SCOPE on ACT_HI_TASKINST(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_TASK_SCOPE_DEF on ACT_HI_TASKINST(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);
alter table ACT_HI_VARINST add SCOPE_ID_ nvarchar(255);
alter table ACT_HI_VARINST add SUB_SCOPE_ID_ nvarchar(255);
alter table ACT_HI_VARINST add SCOPE_TYPE_ nvarchar(255);

create index ACT_IDX_HI_VAR_SCOPE_ID_TYPE on ACT_HI_VARINST(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_VAR_SUB_ID_TYPE on ACT_HI_VARINST(SUB_SCOPE_ID_, SCOPE_TYPE_);
