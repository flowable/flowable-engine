alter table ACT_RU_EVENT_SUBSCR add column SCOPE_DEFINITION_KEY_ varchar(255);

update ACT_GE_PROPERTY set VALUE_ = '7.0.1.1' where NAME_ = 'eventsubscription.schema.version';
