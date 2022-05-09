alter table ACT_RU_EVENT_SUBSCR add column LOCK_TIME_ timestamp;
alter table ACT_RU_EVENT_SUBSCR add column LOCK_OWNER_ varchar(255);

update ACT_GE_PROPERTY set VALUE_ = '6.7.2.2' where NAME_ = 'eventsubscription.schema.version';
