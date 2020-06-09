alter table ACT_RU_ACTINST add TRANSACTION_ORDER_ INTEGER;

update ACT_GE_PROPERTY set VALUE_ = '6.5.1.5' where NAME_ = 'schema.version';
