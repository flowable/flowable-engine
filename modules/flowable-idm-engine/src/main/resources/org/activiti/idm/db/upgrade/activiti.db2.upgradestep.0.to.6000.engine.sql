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
    TOKEN_VALUE_ varchar(255),
    IP_ADDRESS_ varchar(255),
    primary key (ID_)
);
