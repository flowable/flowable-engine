update ACT_GE_PROPERTY set VALUE_ = '6.5.0.3' where NAME_ = 'identitylink.schema.version';

alter table ACT_RU_IDENTITYLINK add SUB_SCOPE_ID_ NVARCHAR2(255);

create index ACT_IDX_IDENT_LNK_SUB_SCOPE on ACT_RU_IDENTITYLINK(SUB_SCOPE_ID_, SCOPE_TYPE_);
