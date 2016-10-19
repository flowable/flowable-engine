create table ACT_ID_PROPERTY (
    NAME_ varchar(64),
    VALUE_ varchar(300),
    REV_ integer,
    primary key (NAME_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

insert into ACT_ID_PROPERTY
values ('schema.version', '6.0.0.0', 1);

insert into ACT_ID_PROPERTY
values ('next.dbid', '9999', 1);

create table ACT_ID_BYTEARRAY (
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    BYTES_ LONGBLOB,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
