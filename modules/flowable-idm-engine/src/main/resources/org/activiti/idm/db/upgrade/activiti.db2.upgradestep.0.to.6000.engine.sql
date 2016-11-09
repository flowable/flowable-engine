create table ACT_ID_PROPERTY (
    NAME_ varchar(64) not null,
    VALUE_ varchar(300),
    REV_ integer,
    primary key (NAME_)
);

insert into ACT_ID_PROPERTY
values ('schema.version', '6.0.0.0', 1);

insert into ACT_ID_PROPERTY
values ('next.dbid', '9999', 1);

create table ACT_ID_BYTEARRAY (
    ID_ varchar(64) not null,
    REV_ integer,
    NAME_ varchar(255),
    BYTES_ BLOB,
    primary key (ID_)
);

create table ACT_ID_TOKEN (
    ID_ varchar(64) not null,
    REV_ integer,
    TOKEN_VALUE_ varchar(255),
    TOKEN_DATE_ timestamp,
    IP_ADDRESS_ varchar(255),
    USER_AGENT_ varchar(255),
    USER_ID_ varchar(255),
    TOKEN_DATA_ varchar(2000),
    primary key (ID_)
);

create table ACT_ID_CAPABILITY (
    ID_ varchar(64) not null,
    USER_ID_ varchar(255),
    GROUP_ID_ varchar(255),
    CAPABILITY_ varchar(255),
    primary key (ID_)
);  
    
create index ACT_IDX_CAP_NAME on ACT_ID_CAPABILITY(CAPABILITY_);
