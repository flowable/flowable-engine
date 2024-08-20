drop index ACT_IDX_CHANNEL_DEF_UNIQ;
drop index ACT_IDX_EVENT_DEF_UNIQ;

drop table FLW_CHANNEL_DEFINITION;
drop table FLW_EVENT_DEFINITION;
drop table FLW_EVENT_RESOURCE;
drop table FLW_EVENT_DEPLOYMENT;

delete from ACT_GE_PROPERTY where NAME_ = 'eventregistry.schema.version';
