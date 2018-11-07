alter table ACT_RU_VARIABLE add SCOPE_ID_ nvarchar(255);
alter table ACT_RU_VARIABLE add SUB_SCOPE_ID_ nvarchar(255);
alter table ACT_RU_VARIABLE add SCOPE_TYPE_ nvarchar(255);

create index ACT_IDX_RU_VAR_SCOPE_ID_TYPE on ACT_RU_VARIABLE(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_RU_VAR_SUB_ID_TYPE on ACT_RU_VARIABLE(SUB_SCOPE_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('variable.schema.version', '6.2.0.0', 1);