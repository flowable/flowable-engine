create table ACT_ID_PROPERTY (
    NAME_ varchar(64) not null,
    VALUE_ varchar(300),
    REV_ integer,
    primary key (NAME_)
);

insert into ACT_ID_PROPERTY
values ('schema.version', '6.3.2.0', 1);

create table ACT_ID_BYTEARRAY (
    ID_ varchar(64) not null,
    REV_ integer,
    NAME_ varchar(191),
    BYTES_ BLOB,
    primary key (ID_)
);

create table ACT_ID_GROUP (
    ID_ varchar(64) not null,
    REV_ integer,
    NAME_ varchar(191),
    TYPE_ varchar(191),
    primary key (ID_)
);

create table ACT_ID_MEMBERSHIP (
    USER_ID_ varchar(64) not null,
    GROUP_ID_ varchar(64) not null,
    primary key (USER_ID_, GROUP_ID_)
);

create table ACT_ID_USER (
    ID_ varchar(64) not null,
    REV_ integer,
    FIRST_ varchar(191),
    LAST_ varchar(191),
    EMAIL_ varchar(191),
    PWD_ varchar(191),
    PICTURE_ID_ varchar(64),
    TENANT_ID_ varchar(191) default '',
    primary key (ID_)
);

create table ACT_ID_INFO (
    ID_ varchar(64) not null,
    REV_ integer,
    USER_ID_ varchar(64),
    TYPE_ varchar(64),
    KEY_ varchar(191),
    VALUE_ varchar(191),
    PASSWORD_ BLOB,
    PARENT_ID_ varchar(191),
    primary key (ID_)
);

create table ACT_ID_TOKEN (
    ID_ varchar(64) not null,
    REV_ integer,
    TOKEN_VALUE_ varchar(191),
    TOKEN_DATE_ timestamp,
    IP_ADDRESS_ varchar(191),
    USER_AGENT_ varchar(191),
    USER_ID_ varchar(191),
    TOKEN_DATA_ varchar(2000),
    primary key (ID_)
);

create table ACT_ID_PRIV (
    ID_ varchar(64) not null,
    NAME_ varchar(191) not null,
    primary key (ID_)
);

create table ACT_ID_PRIV_MAPPING (
    ID_ varchar(64) not null,
    PRIV_ID_ varchar(64) not null,
    USER_ID_ varchar(191),
    GROUP_ID_ varchar(191),
    primary key (ID_)
);

alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_GROUP
    foreign key (GROUP_ID_)
    references ACT_ID_GROUP (ID_);

alter table ACT_ID_MEMBERSHIP
    add constraint ACT_FK_MEMB_USER
    foreign key (USER_ID_)
    references ACT_ID_USER (ID_);

alter table ACT_ID_PRIV_MAPPING
    add constraint ACT_FK_PRIV_MAPPING
    foreign key (PRIV_ID_)
    references ACT_ID_PRIV (ID_);

create index ACT_IDX_PRIV_USER on ACT_ID_PRIV_MAPPING(USER_ID_);
create index ACT_IDX_PRIV_GROUP on ACT_ID_PRIV_MAPPING(GROUP_ID_);
create index ACT_IDX_PRIV_MAPPING on ACT_ID_PRIV_MAPPING(PRIV_ID_);

alter table ACT_ID_PRIV
    add constraint ACT_UNIQ_PRIV_NAME
    unique (NAME_);
