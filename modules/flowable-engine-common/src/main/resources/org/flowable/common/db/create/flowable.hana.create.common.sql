create table ACT_GE_PROPERTY (
    NAME_ NVARCHAR(64),
    VALUE_ NVARCHAR(300),
    REV_ INTEGER,
    primary key (NAME_)
);

create table ACT_GE_BYTEARRAY (
    ID_ NVARCHAR(64),
    REV_ INTEGER,
    NAME_ NVARCHAR(255),
    DEPLOYMENT_ID_ NVARCHAR(64),
    BYTES_ BLOB,
    GENERATED_ NUMBER(1,0) CHECK (GENERATED_ IN (1,0)),
    primary key (ID_)
);

insert into ACT_GE_PROPERTY
values ('common.schema.version', '6.3.2.0', 1);

insert into ACT_GE_PROPERTY
values ('next.dbid', '1', 1);
