create table FLW_RU_BATCH (
    ID_ NVARCHAR2(64) not null,
    REV_ INTEGER,
    TYPE_ NVARCHAR2(64) not null,
    SEARCH_KEY_ NVARCHAR2(255),
    SEARCH_KEY2_ NVARCHAR2(255),
    CREATE_TIME_ TIMESTAMP(6) not null,
    COMPLETE_TIME_ TIMESTAMP(6),
    STATUS_ NVARCHAR2(255),
    BATCH_DOC_ID_ NVARCHAR2(64),
    TENANT_ID_ NVARCHAR2(255) default '',
    primary key (ID_)
);

create table FLW_RU_BATCH_PART (
    ID_ NVARCHAR2(64) not null,
    REV_ INTEGER,
    BATCH_ID_ NVARCHAR2(64),
    TYPE_ NVARCHAR2(64) not null,
    SCOPE_ID_ NVARCHAR2(64),
    SUB_SCOPE_ID_ NVARCHAR2(64),
    SCOPE_TYPE_ NVARCHAR2(64),
    SEARCH_KEY_ NVARCHAR2(255),
    SEARCH_KEY2_ NVARCHAR2(255),
    CREATE_TIME_ TIMESTAMP(6) not null,
    COMPLETE_TIME_ TIMESTAMP(6),
    STATUS_ NVARCHAR2(255),
    RESULT_DOC_ID_ NVARCHAR2(64),
    TENANT_ID_ NVARCHAR2(255) default '',
    primary key (ID_)
);

create index FLW_IDX_BATCH_PART on FLW_RU_BATCH_PART(BATCH_ID_);

alter table FLW_RU_BATCH_PART
    add constraint FLW_FK_BATCH_PART_PARENT
    foreign key (BATCH_ID_)
    references FLW_RU_BATCH (ID_);

insert into ACT_GE_PROPERTY values ('batch.schema.version', '6.7.2.0', 1);
