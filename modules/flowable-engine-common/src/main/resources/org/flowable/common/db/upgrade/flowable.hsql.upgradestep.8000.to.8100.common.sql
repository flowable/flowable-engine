alter table ACT_RU_EVENT_SUBSCR alter column ACTIVITY_ID_ varchar(255);

update ACT_GE_PROPERTY set VALUE_ = '8.1.0.0' where NAME_ = 'common.schema.version';
