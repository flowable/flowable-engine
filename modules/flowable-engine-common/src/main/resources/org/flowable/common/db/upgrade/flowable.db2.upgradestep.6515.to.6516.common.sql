update ACT_GE_PROPERTY set VALUE_ = '6.5.1.6' where NAME_ = 'common.schema.version';


alter table ACT_RU_ENTITYLINK add column SUB_SCOPE_ID_ varchar(255);
alter table ACT_RU_ENTITYLINK add column PARENT_ELEMENT_ID_ varchar(255);

alter table ACT_HI_ENTITYLINK add column SUB_SCOPE_ID_ varchar(255);
alter table ACT_HI_ENTITYLINK add column PARENT_ELEMENT_ID_ varchar(255);

