alter table ACT_RU_EVENT_SUBSCR add LOCK_TIME_ TIMESTAMP(6);
alter table ACT_RU_EVENT_SUBSCR add LOCK_OWNER_ NVARCHAR2(255);

update ACT_GE_PROPERTY set VALUE_ = '6.7.2.2' where NAME_ = 'eventsubscription.schema.version';
