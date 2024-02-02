alter table ACT_RU_EXECUTION add BUSINESS_STATUS_ nvarchar(255);

update ACT_GE_PROPERTY set VALUE_ = '6.7.0.1' where NAME_ = 'schema.version';
