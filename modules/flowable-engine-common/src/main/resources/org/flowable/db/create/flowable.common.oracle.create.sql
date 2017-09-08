create table ACT_GE_PROPERTY (
    NAME_ NVARCHAR2(64),
    VALUE_ NVARCHAR2(300),
    REV_ INTEGER,
    primary key (NAME_)
);

insert into ACT_GE_PROPERTY
values ('common.schema.version', '6.2.0.0', 1);

insert into ACT_GE_PROPERTY
values ('next.dbid', '1', 1);
