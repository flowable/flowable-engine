update ACT_GE_PROPERTY set VALUE_ = '6.5.0.6' where NAME_ = 'common.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.6' where NAME_ = 'entitylink.schema.version';

alter table ACT_HI_IDENTITYLINK add column SUB_SCOPE_ID_ varchar(255);

create index ACT_IDX_HI_IDENT_LNK_SUB_SCOPE on ACT_HI_IDENTITYLINK(SUB_SCOPE_ID_, SCOPE_TYPE_);

alter table ACT_RU_IDENTITYLINK add column SUB_SCOPE_ID_ varchar(255);

create index ACT_IDX_IDENT_LNK_SUB_SCOPE on ACT_RU_IDENTITYLINK(SUB_SCOPE_ID_, SCOPE_TYPE_);

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.6' where NAME_ = 'identitylink.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.6' where NAME_ = 'job.schema.version';

create table FLW_RU_BATCH (
    ID_ varchar(64) not null,
    REV_ integer,
    TYPE_ varchar(64) not null,
    SEARCH_KEY_ varchar(255),
    SEARCH_KEY2_ varchar(255),
    CREATE_TIME_ timestamp not null,
    COMPLETE_TIME_ timestamp,
    STATUS_ varchar(255),
    BATCH_DOC_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
);

create table FLW_RU_BATCH_PART (
    ID_ varchar(64) not null,
    REV_ integer,
    BATCH_ID_ varchar(64),
    TYPE_ varchar(64) not null,
    SCOPE_ID_ varchar(64),
    SUB_SCOPE_ID_ varchar(64),
    SCOPE_TYPE_ varchar(64),
    SEARCH_KEY_ varchar(255),
    SEARCH_KEY2_ varchar(255),
    CREATE_TIME_ timestamp not null,
    COMPLETE_TIME_ timestamp,
    STATUS_ varchar(255),
    RESULT_DOC_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
);

create index FLW_IDX_BATCH_PART on FLW_RU_BATCH_PART(BATCH_ID_);

alter table FLW_RU_BATCH_PART
    add constraint FLW_FK_BATCH_PART_PARENT
    foreign key (BATCH_ID_)
    references FLW_RU_BATCH (ID_);

insert into ACT_GE_PROPERTY values ('batch.schema.version', '6.5.0.6', 1);

alter table ACT_HI_TASKINST add column PROPAGATED_STAGE_INST_ID_ varchar(255);

alter table ACT_RU_TASK add column PROPAGATED_STAGE_INST_ID_ varchar(255);

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.6' where NAME_ = 'task.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.6' where NAME_ = 'variable.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.5.0.6' where NAME_ = 'eventsubscription.schema.version';
