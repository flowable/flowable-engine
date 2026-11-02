update ACT_GE_PROPERTY set VALUE_ = '6.8.1.0' where NAME_ = 'common.schema.version';


alter table ACT_RU_VARIABLE add META_INFO_ NVARCHAR2(2000);

alter table ACT_HI_VARINST add META_INFO_ NVARCHAR2(2000);

