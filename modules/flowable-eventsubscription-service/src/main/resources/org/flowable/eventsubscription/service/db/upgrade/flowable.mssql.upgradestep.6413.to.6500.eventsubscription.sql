alter table ACT_RU_EVENT_SUBSCR add SUB_SCOPE_ID_ nvarchar(64);
alter table ACT_RU_EVENT_SUBSCR add SCOPE_ID_ nvarchar(64);
alter table ACT_RU_EVENT_SUBSCR add SCOPE_DEFINITION_ID_ nvarchar(64);
alter table ACT_RU_EVENT_SUBSCR add SCOPE_TYPE_ nvarchar(64);

insert into ACT_GE_PROPERTY values ('eventsubscription.schema.version', '6.5.0.0', 1);
