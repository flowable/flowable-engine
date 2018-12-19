update ACT_RU_IDENTITYLINK set SCOPE_DEFINITION_ID_ = null where SCOPE_ID_ is not null and SCOPE_DEFINITION_ID_ is not null;

update ACT_GE_PROPERTY set VALUE_ = '6.4.1.2' where NAME_ = 'identitylink.schema.version';
