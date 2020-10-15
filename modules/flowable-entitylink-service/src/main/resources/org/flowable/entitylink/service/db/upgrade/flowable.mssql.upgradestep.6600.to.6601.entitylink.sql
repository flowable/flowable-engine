create index ACT_IDX_ENT_LNK_REF_SCOPE on ACT_RU_ENTITYLINK(REF_SCOPE_ID_, REF_SCOPE_TYPE_, LINK_TYPE_);

update ACT_GE_PROPERTY set VALUE_ = '6.6.0.1' where NAME_ = 'entitylink.schema.version';
