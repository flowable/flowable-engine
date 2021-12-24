create table ACT_GE_PROPERTY (
    NAME_ varchar(64),
    VALUE_ varchar(300),
    REV_ integer,
    primary key (NAME_)
);

create table ACT_GE_BYTEARRAY (
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    DEPLOYMENT_ID_ varchar(64),
    BYTES_ longvarbinary,
    GENERATED_ bit,
    primary key (ID_)
);

insert into ACT_GE_PROPERTY
values ('common.schema.version', '6.7.2.0', 1);

insert into ACT_GE_PROPERTY
values ('next.dbid', '1', 1);
