alter table ACT_HI_TASKINST add column SCOPE_ID_ varchar(255);
alter table ACT_HI_TASKINST add column SUB_SCOPE_ID_ varchar(255);
alter table ACT_HI_TASKINST add column SCOPE_TYPE_ varchar(255);
alter table ACT_HI_TASKINST add column SCOPE_DEFINITION_ID_ varchar(255);

create index ACT_IDX_HI_TASK_SCOPE on ACT_HI_TASKINST(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_TASK_SUB_SCOPE on ACT_HI_TASKINST(SUB_SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_TASK_SCOPE_DEF on ACT_HI_TASKINST(SCOPE_DEFINITION_ID_, SCOPE_TYPE_); 
alter table ACT_HI_VARINST add column SCOPE_ID_ varchar(255);
alter table ACT_HI_VARINST add column SUB_SCOPE_ID_ varchar(255);
alter table ACT_HI_VARINST add column SCOPE_TYPE_ varchar(255);

create index ACT_IDX_HI_VAR_SCOPE_ID_TYPE on ACT_HI_VARINST(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_VAR_SUB_ID_TYPE on ACT_HI_VARINST(SUB_SCOPE_ID_, SCOPE_TYPE_);

