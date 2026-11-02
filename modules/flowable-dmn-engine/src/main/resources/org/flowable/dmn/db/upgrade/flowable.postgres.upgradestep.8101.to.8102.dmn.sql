alter table ACT_DMN_DECISION alter column DESCRIPTION_ TYPE varchar(4000);

update ACT_GE_PROPERTY set VALUE_ = '8.1.0.2' where NAME_ = 'dmn.schema.version';
