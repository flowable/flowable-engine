alter table FLW_EVENT_DEFINITION alter column DESCRIPTION_ SET DATA TYPE varchar(4000);

Call Sysproc.admin_cmd ('REORG TABLE FLW_EVENT_DEFINITION');

alter table FLW_CHANNEL_DEFINITION alter column DESCRIPTION_ SET DATA TYPE varchar(4000);

Call Sysproc.admin_cmd ('REORG TABLE FLW_CHANNEL_DEFINITION');

update ACT_GE_PROPERTY set VALUE_ = '8.1.0.2' where NAME_ = 'eventregistry.schema.version';
