create table ACT_GE_PROPERTY (
    NAME_ nvarchar(64),
    VALUE_ nvarchar(300),
    REV_ int,
    primary key (NAME_)
);

insert into ACT_GE_PROPERTY
values ('common.schema.version', '6.2.0.0', 1);

insert into ACT_GE_PROPERTY
values ('next.dbid', '1', 1);
