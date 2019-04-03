alter table ACT_RU_EVENT_SUBSCR add column SUB_SCOPE_ID_ varchar(64);
alter table ACT_RU_EVENT_SUBSCR add column SCOPE_ID_ varchar(64);
alter table ACT_RU_EVENT_SUBSCR add column SCOPE_DEFINITION_ID_ varchar(64);
alter table ACT_RU_EVENT_SUBSCR add column SCOPE_TYPE_ varchar(64);

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.0' where NAME_ = 'eventsubscription.schema.version';