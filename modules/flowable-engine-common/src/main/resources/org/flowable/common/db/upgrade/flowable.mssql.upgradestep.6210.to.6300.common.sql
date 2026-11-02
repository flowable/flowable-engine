update ACT_GE_PROPERTY set VALUE_ = '6.3.0.0' where NAME_ = 'common.schema.version';


alter table ACT_RU_IDENTITYLINK add SCOPE_ID_ nvarchar(255);
alter table ACT_RU_IDENTITYLINK add SCOPE_TYPE_ nvarchar(255);
alter table ACT_RU_IDENTITYLINK add SCOPE_DEFINITION_ID_ nvarchar(255);
create index ACT_IDX_IDENT_LNK_SCOPE on ACT_RU_IDENTITYLINK(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_IDENT_LNK_SCOPE_DEF on ACT_RU_IDENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

alter table ACT_HI_IDENTITYLINK add SCOPE_ID_ nvarchar(255);
alter table ACT_HI_IDENTITYLINK add SCOPE_TYPE_ nvarchar(255);
alter table ACT_HI_IDENTITYLINK add SCOPE_DEFINITION_ID_ nvarchar(255);

create index ACT_IDX_HI_IDENT_LNK_SCOPE on ACT_HI_IDENTITYLINK(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_HI_IDENT_LNK_SCOPE_DEF on ACT_HI_IDENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

update ACT_RU_TIMER_JOB set HANDLER_TYPE_ = 'cmmn-trigger-timer' where HANDLER_TYPE_ = 'trigger-timer' and SCOPE_TYPE_ = 'cmmn';

