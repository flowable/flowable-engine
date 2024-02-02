update ACT_GE_PROPERTY set VALUE_ = '6.0.1.0' where NAME_ = 'schema.version';

update ACT_ID_PROPERTY set VALUE_ = '6.0.1.0' where NAME_ = 'schema.version';
     
create index ACT_IDX_PRIV_MAPPING on ACT_ID_PRIV_MAPPING(PRIV_ID_);
