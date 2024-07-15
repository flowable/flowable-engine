update ACT_GE_PROPERTY set VALUE_ = '6.8.1.0' where NAME_ = 'common.schema.version';


alter table ACT_RU_VARIABLE add column META_INFO_ varchar(4000);

alter table ACT_HI_VARINST add column META_INFO_ varchar(4000);

