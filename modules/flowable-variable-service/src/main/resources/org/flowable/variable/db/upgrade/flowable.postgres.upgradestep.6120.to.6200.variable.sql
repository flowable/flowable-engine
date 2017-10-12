alter table ACT_RU_VARIABLE add column SCOPE_ID_ varchar(255);
alter table ACT_RU_VARIABLE add column SUB_SCOPE_ID_ varchar(255);
alter table ACT_RU_VARIABLE add column SCOPE_TYPE_ varchar(255);

create index ACT_IDX_RU_VAR_SCOPE_ID_TYPE on ACT_RU_VARIABLE(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_RU_VAR_SUB_ID_TYPE on ACT_RU_VARIABLE(SUB_SCOPE_ID_, SCOPE_TYPE_);

insert into ACT_GE_PROPERTY values ('variable.schema.version', '6.2.0.0', 1);
