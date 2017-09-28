alter table ACT_HI_VARINST add column SCOPE_ID_ nvarchar(255);
alter table ACT_HI_VARINST add column SUB_SCOPE_ID_ nvarchar(255);
alter table ACT_HI_VARINST add column SCOPE_TYPE_ nvarchar(255);

create index ACT_IDX_HI_VAR_SCOPE_ID_TYPE on ACT_HI_VARINST(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_VAR_SUB_SCOPE_ID_TYPE on ACT_HI_VARINST(SUB_SCOPE_ID_, SCOPE_TYPE_);