alter table FLW_EVENT_DEFINITION modify DESCRIPTION_ VARCHAR2(4000);

alter table FLW_CHANNEL_DEFINITION modify DESCRIPTION_ VARCHAR2(4000);

update ACT_GE_PROPERTY set VALUE_ = '8.1.0.2' where NAME_ = 'eventregistry.schema.version';
