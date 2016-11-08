create table ACT_ID_PROPERTY (
    NAME_ nvarchar(64),
    VALUE_ nvarchar(300),
    REV_ int,
    primary key (NAME_)
);

insert into ACT_ID_PROPERTY
values ('schema.version', '6.0.0.0', 1);

insert into ACT_ID_PROPERTY
values ('next.dbid', '9999', 1);

create table ACT_ID_BYTEARRAY (
    ID_ nvarchar(64),
    REV_ int,
    NAME_ nvarchar(255),
    BYTES_  varbinary(max),
    primary key (ID_)
);

create table ACT_ID_TOKEN (
    ID_ nvarchar(64) not null,
    REV_ int,
    TOKEN_VALUE_ nvarchar(255),
    TOKEN_DATE_ datetime,
    IP_ADDRESS_ nvarchar(255),
    USER_AGENT_ nvarchar(255),
    USER_ID_ nvarchar(255),
    TOKEN_DATA_ nvarchar(2000),
    primary key (ID_)
);

create table ACT_ID_CAPABILITY (
    ID_ nvarchar(64) not null,
    USER_ID_ nvarchar(255),
    GROUP_ID_ nvarchar(255),
    CAPABILITY_ nvarchar(255),
    primary key (ID_)
);  
    
create index ACT_IDX_CAP_NAME on ACT_ID_CAPABILITY(CAPABILITY_);
