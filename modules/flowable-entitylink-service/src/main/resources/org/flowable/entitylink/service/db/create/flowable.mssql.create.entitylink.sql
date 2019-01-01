create table ACT_RU_ENTITYLINK (
    ID_ nvarchar(64),
    REV_ int,
    CREATE_TIME_ datetime,
    LINK_TYPE_ nvarchar(255),
    SCOPE_ID_ nvarchar(255),
    SCOPE_TYPE_ nvarchar(255),
    SCOPE_DEFINITION_ID_ nvarchar(255),
    REF_SCOPE_ID_ nvarchar(255),
    REF_SCOPE_TYPE_ nvarchar(255),
    REF_SCOPE_DEFINITION_ID_ nvarchar(255),
    HIERARCHY_TYPE_ nvarchar(255),
    primary key (ID_)
);

create index ACT_IDX_ENT_LNK_SCOPE on ACT_RU_ENTITYLINK(SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_ENT_LNK_SCOPE_DEF on ACT_RU_ENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);

insert into ACT_GE_PROPERTY values ('entitylink.schema.version', '6.4.1.3', 1);