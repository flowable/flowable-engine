update ACT_ID_PROPERTY set VALUE_ = '6.3.0.0' where NAME_ = 'schema.version';

alter table ACT_ID_USER add column
    TENANT_ID_ varchar(255) default '';
