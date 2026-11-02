update ACT_GE_PROPERTY set VALUE_ = '6.4.1.1' where NAME_ = 'common.schema.version';


alter table ACT_RU_ENTITYLINK add HIERARCHY_TYPE_ NVARCHAR2(255);

alter table ACT_HI_ENTITYLINK add HIERARCHY_TYPE_ NVARCHAR2(255);

