create table ACT_ID_PROPERTY (
    NAME_ nvarchar(64),
    VALUE_ nvarchar(300),
    REV_ int,
    primary key (NAME_)
);

insert into ACT_ID_PROPERTY
values ('schema.version', '6.0.0.0', 1);

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

create table ACT_ID_PRIV (
    ID_ nvarchar(64) not null,
    NAME_ nvarchar(255),
    primary key (ID_)
);

create table ACT_ID_PRIV_MAPPING (
    ID_ nvarchar(64) not null,
    PRIV_ID_ nvarchar(64) not null,
    USER_ID_ nvarchar(255),
    GROUP_ID_ nvarchar(255),
    primary key (ID_)
);

alter table ACT_ID_PRIV_MAPPING 
    add constraint ACT_FK_PRIV_MAPPING 
    foreign key (PRIV_ID_) 
    references ACT_ID_PRIV (ID_);
    
create index ACT_IDX_PRIV_USER on ACT_ID_PRIV_MAPPING(USER_ID_);
create index ACT_IDX_PRIV_GROUP on ACT_ID_PRIV_MAPPING(GROUP_ID_);  
alter table ACT_RE_PROCDEF add ENGINE_VERSION_ nvarchar(255);
update ACT_RE_PROCDEF set ENGINE_VERSION_ = 'v5';

alter table ACT_RE_DEPLOYMENT add ENGINE_VERSION_ nvarchar(255);
update ACT_RE_DEPLOYMENT set ENGINE_VERSION_ = 'v5';

alter table ACT_RU_EXECUTION add ROOT_PROC_INST_ID_ nvarchar(64);
create index ACT_IDX_EXEC_ROOT on ACT_RU_EXECUTION(ROOT_PROC_INST_ID_);

update ACT_GE_PROPERTY set VALUE_ = '6.0.0.0' where NAME_ = 'schema.version';

