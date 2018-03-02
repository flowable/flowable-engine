update ACT_GE_PROPERTY set VALUE_ = '6.3.0.0' where NAME_ = 'identitylink.schema.version';

alter table ACT_RU_IDENTITYLINK add column SCOPE_ID_ varchar(255);
alter table ACT_RU_IDENTITYLINK add column SCOPE_TYPE_ varchar(255);
alter table ACT_RU_IDENTITYLINK add column SCOPE_DEFINITION_ID_ varchar(255);

create index ACT_IDX_IDENT_LNK_SCOPE on ACT_RU_IDENTITYLINK(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_IDENT_LNK_SCOPE_DEF on ACT_RU_IDENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);
