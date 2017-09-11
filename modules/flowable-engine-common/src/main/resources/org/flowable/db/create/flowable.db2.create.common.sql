create table ACT_GE_PROPERTY (
    NAME_ varchar(64) not null,
    VALUE_ varchar(300),
    REV_ integer,
    primary key (NAME_)
);

insert into ACT_GE_PROPERTY
values ('common.schema.version', '6.2.0.0', 1);

insert into ACT_GE_PROPERTY
values ('next.dbid', '1', 1);
