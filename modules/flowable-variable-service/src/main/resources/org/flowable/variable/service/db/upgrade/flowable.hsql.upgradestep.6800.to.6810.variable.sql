alter table ACT_RU_VARIABLE add column META_INFO_ varchar(4000);

update ACT_GE_PROPERTY set VALUE_ = '6.8.1.0' where NAME_ = 'variable.schema.version';
