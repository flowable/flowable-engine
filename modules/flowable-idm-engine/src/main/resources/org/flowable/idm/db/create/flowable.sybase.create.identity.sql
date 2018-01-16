create table ACT_ID_PROPERTY (
    NAME_ varchar(64) not null,
    VALUE_ varchar(300) null,
    REV_ int null,
    primary key (NAME_)
);

insert into ACT_ID_PROPERTY
values ('schema.version', '6.3.0.0', 1);

create table ACT_ID_BYTEARRAY (
    ID_ varchar(64) not null,
    REV_ int null,
    NAME_ varchar(255) null,
    BYTES_ IMAGE null,
    primary key (ID_)
);

create table ACT_ID_GROUP (
    ID_ varchar(64) not null,
    REV_ int null,
    NAME_ varchar(255) null,
    TYPE_ varchar(255) null,
    primary key (ID_)
);

create table ACT_ID_MEMBERSHIP (
    USER_ID_ varchar(64) not null,
    GROUP_ID_ varchar(64) not null,
    primary key (USER_ID_, GROUP_ID_)
);

create table ACT_ID_USER (
    ID_ varchar(64) not null,
    REV_ int null,
    FIRST_ varchar(255) null,
    LAST_ varchar(255) null,
    EMAIL_ varchar(255) null,
    PWD_ varchar(255) null,
    PICTURE_ID_ varchar(64) null,
    primary key (ID_)
);

create table ACT_ID_INFO (
    ID_ varchar(64) not null,
    REV_ int null,
    USER_ID_ varchar(64) null,
    TYPE_ varchar(64) null,
    KEY_ varchar(255) null,
    VALUE_ varchar(255) null,
    PASSWORD_ IMAGE null,
    PARENT_ID_ varchar(255) null,
    primary key (ID_)
);

create table ACT_ID_TOKEN (
    ID_ varchar(64) not null,
    REV_ int,
    TOKEN_VALUE_ varchar(255) null,
    TOKEN_DATE_ timestamp null,
    IP_ADDRESS_ varchar(255) null,
    USER_AGENT_ varchar(255) null,
    USER_ID_ varchar(255) null,
    TOKEN_DATA_ varchar(2000) null,
    primary key (ID_)
);

create table ACT_ID_PRIV (
    ID_ varchar(64) not null,
    NAME_ varchar(255) null,
    primary key (ID_)
);

create table ACT_ID_PRIV_MAPPING (
    ID_ varchar(64) not null,
    PRIV_ID_ varchar(64) not null,
    USER_ID_ varchar(255) null,
    GROUP_ID_ varchar(255) null,
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