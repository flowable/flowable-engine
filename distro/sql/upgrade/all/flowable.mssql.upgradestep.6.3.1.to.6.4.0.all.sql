alter table ACT_ID_USER add DISPLAY_NAME_ nvarchar(255) default '';

update ACT_ID_PROPERTY set VALUE_ = '6.4.0.0' where NAME_ = 'schema.version';
