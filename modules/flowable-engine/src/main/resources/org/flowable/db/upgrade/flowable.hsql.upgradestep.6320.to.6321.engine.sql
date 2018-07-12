alter table ACT_RU_EXECUTION add DYNAMIC_STATE_ varchar(255);

update ACT_GE_PROPERTY set VALUE_ = '6.3.2.1' where NAME_ = 'schema.version';
