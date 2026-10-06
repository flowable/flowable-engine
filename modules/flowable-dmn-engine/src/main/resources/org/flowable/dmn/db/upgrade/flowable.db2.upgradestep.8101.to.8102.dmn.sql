alter table ACT_DMN_DECISION alter column DESCRIPTION_ SET DATA TYPE varchar(4000);

Call Sysproc.admin_cmd ('REORG TABLE ACT_DMN_DECISION');

update ACT_GE_PROPERTY set VALUE_ = '8.1.0.2' where NAME_ = 'dmn.schema.version';
