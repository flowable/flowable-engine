alter table ACT_RU_ENTITYLINK add column ROOT_SCOPE_ID_ nvarchar(255);
alter table ACT_RU_ENTITYLINK add column ROOT_SCOPE_TYPE_ nvarchar(255);
alter table ACT_RU_ENTITYLINK add column ROOT_SCOPE_DEFINITION_ID_ nvarchar(255);

update ACT_GE_PROPERTY set VALUE_ = '6.4.1.1' where NAME_ = 'entitylink.schema.version';
