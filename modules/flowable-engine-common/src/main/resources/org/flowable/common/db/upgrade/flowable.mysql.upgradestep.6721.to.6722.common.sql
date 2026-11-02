update ACT_GE_PROPERTY set VALUE_ = '6.7.2.2' where NAME_ = 'common.schema.version';


alter table ACT_RU_EVENT_SUBSCR add column LOCK_TIME_ timestamp(3) NULL;
alter table ACT_RU_EVENT_SUBSCR add column LOCK_OWNER_ varchar(255);

