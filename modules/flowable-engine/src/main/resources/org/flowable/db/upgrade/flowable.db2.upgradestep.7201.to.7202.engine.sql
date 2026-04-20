create index ACT_IDX_BYTEAR_DEPL on ACT_GE_BYTEARRAY(DEPLOYMENT_ID_);

update ACT_GE_PROPERTY set VALUE_ = '7.2.0.2' where NAME_ = 'schema.version';
