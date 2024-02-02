alter table ACT_ID_USER add column DISPLAY_NAME_ varchar(255) default '';

update ACT_ID_PROPERTY set VALUE_ = '6.3.2.0' where NAME_ = 'schema.version';
