alter table ACT_RU_EXECUTION add column BUSINESS_STATUS_ varchar(255);

update ACT_GE_PROPERTY set VALUE_ = '6.7.0.1' where NAME_ = 'schema.version';
