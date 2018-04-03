update ACT_ID_PROPERTY set VALUE_ = '6.3.0.1' where NAME_ = 'schema.version';
update ACT_GE_PROPERTY set VALUE_ = '6.3.0.1' where NAME_ = 'identitylink.schema.version';

alter table ACT_RU_TASK add column TASK_DEF_ID_ varchar(64);

update ACT_GE_PROPERTY set VALUE_ = '6.3.0.1' where NAME_ = 'task.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.3.0.1' where NAME_ = 'variable.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.3.0.1' where NAME_ = 'job.schema.version';
alter table ACT_RE_DEPLOYMENT add column DERIVED_FROM_ varchar(64);
alter table ACT_RE_DEPLOYMENT add column DERIVED_FROM_ROOT_ varchar(64);
alter table ACT_RE_PROCDEF add column DERIVED_FROM_ varchar(64);
alter table ACT_RE_PROCDEF add column DERIVED_FROM_ROOT_ varchar(64);

alter table ACT_RE_PROCDEF add column DERIVED_VERSION_ integer not null default 0;

alter table ACT_RE_PROCDEF
    drop constraint ACT_UNIQ_PROCDEF;
    
alter table ACT_RE_PROCDEF
    add constraint ACT_UNIQ_PROCDEF
    unique (KEY_,VERSION_, DERIVED_VERSION_, TENANT_ID_);

update ACT_GE_PROPERTY set VALUE_ = '6.3.0.1' where NAME_ = 'schema.version';

alter table ACT_HI_TASKINST add column TASK_DEF_ID_ varchar(64);

