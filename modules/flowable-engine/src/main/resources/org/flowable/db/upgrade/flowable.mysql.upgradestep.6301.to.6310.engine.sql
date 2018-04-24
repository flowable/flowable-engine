alter table ACT_RE_DEPLOYMENT add column PARENT_DEPLOYMENT_ID_ varchar(255);

update ACT_GE_PROPERTY set VALUE_ = '6.3.1.0' where NAME_ = 'schema.version';
