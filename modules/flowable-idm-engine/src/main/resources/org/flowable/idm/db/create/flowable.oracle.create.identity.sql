create table ACT_ID_PROPERTY (
    NAME_ NVARCHAR2(64),
    VALUE_ NVARCHAR2(300),
    REV_ INTEGER,
    primary key (NAME_)
);

insert into ACT_ID_PROPERTY
values ('schema.version', '6.3.0.0', 1);

create table ACT_ID_BYTEARRAY (
    ID_ NVARCHAR2(64),
    REV_ INTEGER,
    NAME_ NVARCHAR2(255),
    BYTES_ BLOB,
    primary key (ID_)
);

create table ACT_ID_GROUP (
    ID_ NVARCHAR2(64),
    REV_ INTEGER,
    NAME_ NVARCHAR2(255),
    TYPE_ NVARCHAR2(255),
    primary key (ID_)
);

create table ACT_ID_MEMBERSHIP (
    USER_ID_ NVARCHAR2(64),
    GROUP_ID_ NVARCHAR2(64),
    primary key (USER_ID_, GROUP_ID_)
);

create table ACT_ID_USER (
    ID_ NVARCHAR2(64),
    REV_ INTEGER,
    FIRST_ NVARCHAR2(255),
    LAST_ NVARCHAR2(255),
    EMAIL_ NVARCHAR2(255),
    PWD_ NVARCHAR2(255),
    PICTURE_ID_ NVARCHAR2(64),
    TENANT_ID_ NVARCHAR2(255) default '',
    primary key (ID_)
);

create table ACT_ID_INFO (
    ID_ NVARCHAR2(64),
    REV_ INTEGER,
    USER_ID_ NVARCHAR2(64),
    TYPE_ NVARCHAR2(64),
    KEY_ NVARCHAR2(255),
    VALUE_ NVARCHAR2(255),
    PASSWORD_ BLOB,
    PARENT_ID_ NVARCHAR2(255),
    primary key (ID_)
);

create table ACT_ID_TOKEN (
    ID_ NVARCHAR2(64) not null,
    REV_ INTEGER,
    TOKEN_VALUE_ NVARCHAR2(255),
    TOKEN_DATE_ TIMESTAMP(6),
    IP_ADDRESS_ NVARCHAR2(255),
    USER_AGENT_ NVARCHAR2(255),
    USER_ID_ NVARCHAR2(255),
    TOKEN_DATA_ NVARCHAR2(2000),
    primary key (ID_)
);

create table ACT_ID_PRIV (
    ID_ NVARCHAR2(64) not null,
    NAME_ NVARCHAR2(255) not null,
    primary key (ID_)
);

create table ACT_ID_PRIV_MAPPING (
    ID_ NVARCHAR2(64) not null,
    PRIV_ID_ NVARCHAR2(64) not null,
    USER_ID_ NVARCHAR2(255),
    GROUP_ID_ NVARCHAR2(255),
    primary key (ID_)
);

create index ACT_IDX_MEMB_GROUP on ACT_ID_MEMBERSHIP(GROUP_ID_);
alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_GROUP
    foreign key (GROUP_ID_)
    references ACT_ID_GROUP (ID_);

create index ACT_IDX_MEMB_USER on ACT_ID_MEMBERSHIP(USER_ID_);
alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_USER
    foreign key (USER_ID_)
    references ACT_ID_USER (ID_);

create index ACT_IDX_PRIV_MAPPING on ACT_ID_PRIV_MAPPING(PRIV_ID_);
alter table ACT_ID_PRIV_MAPPING
    add constraint ACT_FK_PRIV_MAPPING
    foreign key (PRIV_ID_)
    references ACT_ID_PRIV (ID_);

create index ACT_IDX_PRIV_USER on ACT_ID_PRIV_MAPPING(USER_ID_);
create index ACT_IDX_PRIV_GROUP on ACT_ID_PRIV_MAPPING(GROUP_ID_);

alter table ACT_ID_PRIV
    add constraint ACT_UNIQ_PRIV_NAME
    unique (NAME_);
