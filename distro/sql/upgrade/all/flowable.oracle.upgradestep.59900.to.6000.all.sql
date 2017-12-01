create table ACT_ID_PROPERTY (
    NAME_ NVARCHAR2(64),
    VALUE_ NVARCHAR2(300),
    REV_ INTEGER,
    primary key (NAME_)
);

insert into ACT_ID_PROPERTY
values ('schema.version', '6.0.0.0', 1);

create table ACT_ID_BYTEARRAY (
    ID_ NVARCHAR2(64),
    REV_ INTEGER,
    NAME_ NVARCHAR2(255),
    BYTES_ BLOB,
    primary key (ID_)
);

create table ACT_ID_TOKEN (
    ID_ NVARCHAR2(64) not null,
    REV_ INTEGER,
    TOKEN_VALUE_ NVARCHAR2(255),
    TOKEN_DATE_ TIMESTAMP(6),
    IP_ADDRESS_ NVARCHAR2(255),
    USER_AGENT_ NVARCHAR2(255),
    USER_ID_ NVARCHAR2(255),
    TOKEN_DATA_ NVARCHAR2(2000),
    primary key (ID_)
);

create table ACT_ID_PRIV (
    ID_ NVARCHAR2(64) not null,
    NAME_ NVARCHAR2(255),
    primary key (ID_)
);

create table ACT_ID_PRIV_MAPPING (
    ID_ NVARCHAR2(64) not null,
    PRIV_ID_ NVARCHAR2(64) not null,
    USER_ID_ NVARCHAR2(255),
    GROUP_ID_ NVARCHAR2(255),
    primary key (ID_)
);

create index ACT_IDX_PRIV_MAPPING on ACT_ID_PRIV_MAPPING(PRIV_ID_);    
alter table ACT_ID_PRIV_MAPPING 
    add constraint ACT_FK_PRIV_MAPPING 
    foreign key (PRIV_ID_) 
    references ACT_ID_PRIV (ID_);
    
create index ACT_IDX_PRIV_USER on ACT_ID_PRIV_MAPPING(USER_ID_);
create index ACT_IDX_PRIV_GROUP on ACT_ID_PRIV_MAPPING(GROUP_ID_);   
alter table ACT_RE_PROCDEF add (ENGINE_VERSION_ NVARCHAR2(255));
update ACT_RE_PROCDEF set ENGINE_VERSION_ = 'v5';

alter table ACT_RE_DEPLOYMENT add (ENGINE_VERSION_ NVARCHAR2(255));
update ACT_RE_DEPLOYMENT set ENGINE_VERSION_ = 'v5';

alter table ACT_RU_EXECUTION add (ROOT_PROC_INST_ID_ NVARCHAR2(64));
create index ACT_IDX_EXEC_ROOT on ACT_RU_EXECUTION(ROOT_PROC_INST_ID_);    

update ACT_GE_PROPERTY set VALUE_ = '6.0.0.0' where NAME_ = 'schema.version';

