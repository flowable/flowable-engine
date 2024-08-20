drop table if exists FLW_CHANNEL_DEFINITION cascade;
drop table if exists FLW_EVENT_DEFINITION cascade;
drop table if exists FLW_EVENT_RESOURCE cascade;
drop table if exists FLW_EVENT_DEPLOYMENT cascade;

delete from ACT_GE_PROPERTY where NAME_ = 'eventregistry.schema.version';
