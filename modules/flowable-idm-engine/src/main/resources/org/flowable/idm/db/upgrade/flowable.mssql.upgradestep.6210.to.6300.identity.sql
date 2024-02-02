alter table ACT_ID_USER add TENANT_ID_ nvarchar(255) default '';

alter table ACT_ID_PRIV alter column NAME_ nvarchar(255) not null;
alter table ACT_ID_PRIV add constraint ACT_UNIQ_PRIV_NAME unique (NAME_);

update ACT_ID_PROPERTY set VALUE_ = '6.3.0.0' where NAME_ = 'schema.version';
