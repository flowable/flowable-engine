alter table ACT_RU_ENTITYLINK add column HIERARCHY_TYPE_ varchar(255);

update ACT_GE_PROPERTY set VALUE_ = '6.4.1.1' where NAME_ = 'entitylink.schema.version';
