update ACT_GE_PROPERTY set VALUE_ = '6.5.0.1' where NAME_ = 'common.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.1' where NAME_ = 'entitylink.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.1' where NAME_ = 'identitylink.schema.version';

alter table ACT_RU_JOB add column ELEMENT_ID_ varchar(255);
alter table ACT_RU_JOB add column ELEMENT_NAME_ varchar(255);

alter table ACT_RU_TIMER_JOB add column ELEMENT_ID_ varchar(255);
alter table ACT_RU_TIMER_JOB add column ELEMENT_NAME_ varchar(255);

alter table ACT_RU_SUSPENDED_JOB add column ELEMENT_ID_ varchar(255);
alter table ACT_RU_SUSPENDED_JOB add column ELEMENT_NAME_ varchar(255);

alter table ACT_RU_DEADLETTER_JOB add column ELEMENT_ID_ varchar(255);
alter table ACT_RU_DEADLETTER_JOB add column ELEMENT_NAME_ varchar(255);

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.1' where NAME_ = 'job.schema.version';
update ACT_GE_PROPERTY set VALUE_ = '6.5.0.1' where NAME_ = 'task.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.1' where NAME_ = 'variable.schema.version';

alter table ACT_RU_EVENT_SUBSCR add column SUB_SCOPE_ID_ varchar(64);
alter table ACT_RU_EVENT_SUBSCR add column SCOPE_ID_ varchar(64);
alter table ACT_RU_EVENT_SUBSCR add column SCOPE_DEFINITION_ID_ varchar(64);
alter table ACT_RU_EVENT_SUBSCR add column SCOPE_TYPE_ varchar(64);
insert into ACT_GE_PROPERTY values ('eventsubscription.schema.version', '6.5.0.0', 1);

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.1' where NAME_ = 'eventsubscription.schema.version';

