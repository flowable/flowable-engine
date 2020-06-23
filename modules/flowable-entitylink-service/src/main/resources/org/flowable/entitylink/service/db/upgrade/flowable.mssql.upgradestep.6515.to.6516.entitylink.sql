alter table ACT_RU_ENTITYLINK add SUB_SCOPE_ID_ nvarchar(255);
alter table ACT_RU_ENTITYLINK add PARENT_ELEMENT_ID_ nvarchar(255);

update ACT_GE_PROPERTY set VALUE_ = '6.5.1.6' where NAME_ = 'entitylink.schema.version';
