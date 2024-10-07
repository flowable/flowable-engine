create index ACT_IDX_ACT_HI_TSK_LOG_TASK on ACT_HI_TSK_LOG(TASK_ID_);

delete from ACT_GE_PROPERTY where NAME_ = 'batch.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'entitylink.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'eventsubscription.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'identitylink.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'job.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'task.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'variable.schema.version';

create index ACT_IDX_EVENT_SUBSCR_EXEC_ID on ACT_RU_EVENT_SUBSCR(EXECUTION_ID_);
create index ACT_IDX_EVENT_SUBSCR_PROC_ID on ACT_RU_EVENT_SUBSCR(PROC_INST_ID_);

update ACT_GE_PROPERTY set VALUE_ = '7.1.0.2' where NAME_ = 'common.schema.version';

insert into ACT_GE_PROPERTY
values ('app.schema.version', '7.1.0.2', 1);

drop table ACT_APP_DATABASECHANGELOG;
drop table ACT_APP_DATABASECHANGELOGLOCK;

insert into ACT_GE_PROPERTY
values ('cmmn.schema.version', '7.1.0.2', 1);

drop table ACT_CMMN_DATABASECHANGELOG;
drop table ACT_CMMN_DATABASECHANGELOGLOCK;

insert into ACT_GE_PROPERTY
values ('dmn.schema.version', '7.1.0.2', 1);

drop table ACT_DMN_DATABASECHANGELOG;
drop table ACT_DMN_DATABASECHANGELOGLOCK;

insert into ACT_GE_PROPERTY
values ('eventregistry.schema.version', '7.1.0.2', 1);

drop table FLW_EV_DATABASECHANGELOG;
drop table FLW_EV_DATABASECHANGELOGLOCK;

update ACT_GE_PROPERTY set VALUE_ = '7.1.0.2' where NAME_ = 'schema.version';

update ACT_ID_PROPERTY set VALUE_ = '7.1.0.2' where NAME_ = 'schema.version';
