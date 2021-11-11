alter table ACT_RU_EXECUTION add BUSINESS_STATUS_ NVARCHAR2(255);

alter table ACT_HI_PROCINST add BUSINESS_STATUS_ NVARCHAR2(255);

update ACT_GE_PROPERTY set VALUE_ = '6.7.1.0' where NAME_ = 'schema.version';
