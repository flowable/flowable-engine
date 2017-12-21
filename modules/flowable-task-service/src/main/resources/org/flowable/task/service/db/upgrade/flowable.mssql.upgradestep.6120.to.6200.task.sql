alter table ACT_RU_TASK add SCOPE_ID_ nvarchar(255);
alter table ACT_RU_TASK add SUB_SCOPE_ID_ nvarchar(255);
alter table ACT_RU_TASK add SCOPE_TYPE_ nvarchar(255);
alter table ACT_RU_TASK add SCOPE_DEFINITION_ID_ nvarchar(255);

create index ACT_IDX_TASK_SCOPE on ACT_RU_TASK(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TASK_SUB_SCOPE on ACT_RU_TASK(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_TASK_SCOPE_DEF on ACT_RU_TASK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('task.schema.version', '6.2.0.0', 1);