create table FLW_RU_BATCH (
    ID_ nvarchar(64) not null,
    REV_ int,
    TYPE_ nvarchar(64) not null,
    SEARCH_KEY_ nvarchar(255),
    SEARCH_KEY2_ nvarchar(255),
    CREATE_TIME_ datetime not null,
    COMPLETE_TIME_ datetime,
    STATUS_ nvarchar(255),
    BATCH_DOC_ID_ nvarchar(64),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create table FLW_RU_BATCH_PART (
    ID_ nvarchar(64) not null,
    REV_ int,
    BATCH_ID_ nvarchar(64),
    TYPE_ nvarchar(64) not null,
    SCOPE_ID_ nvarchar(64),
    SUB_SCOPE_ID_ nvarchar(64),
    SCOPE_TYPE_ nvarchar(64),
    SEARCH_KEY_ nvarchar(255),
    SEARCH_KEY2_ nvarchar(255),
    CREATE_TIME_ datetime not null,
    COMPLETE_TIME_ datetime,
    STATUS_ nvarchar(255),
    RESULT_DOC_ID_ nvarchar(64),
    TENANT_ID_ nvarchar(255) default '',
    primary key (ID_)
);

create index FLW_IDX_BATCH_PART on FLW_RU_BATCH_PART(BATCH_ID_);

alter table FLW_RU_BATCH_PART
    add constraint FLW_FK_BATCH_PART_PARENT
    foreign key (BATCH_ID_)
    references FLW_RU_BATCH (ID_);

insert into ACT_GE_PROPERTY values ('batch.schema.version', '6.5.0.2', 1);
