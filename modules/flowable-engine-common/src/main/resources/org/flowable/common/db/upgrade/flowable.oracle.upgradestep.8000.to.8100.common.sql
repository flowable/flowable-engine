alter table ACT_RU_EVENT_SUBSCR modify ACTIVITY_ID_ NVARCHAR2(255);

update ACT_GE_PROPERTY set VALUE_ = '8.1.0.0' where NAME_ = 'common.schema.version';
