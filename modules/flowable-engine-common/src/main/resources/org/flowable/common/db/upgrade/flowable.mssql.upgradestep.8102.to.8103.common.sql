create index ACT_IDX_HI_IDENT_LNK_GROUP on ACT_HI_IDENTITYLINK(GROUP_ID_);

update ACT_GE_PROPERTY set VALUE_ = '8.1.0.3' where NAME_ = 'common.schema.version';
