update ACT_ID_PROPERTY set VALUE_ = '6.3.0.0' where NAME_ = 'schema.version';

ALTER TABLE ACT_ID_USER ADD TENANT_ID_ varchar(255) default '';
