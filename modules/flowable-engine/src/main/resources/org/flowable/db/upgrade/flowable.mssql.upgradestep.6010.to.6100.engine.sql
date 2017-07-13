update ACT_GE_PROPERTY set VALUE_ = '6.1.0.0' where NAME_ = 'schema.version';

alter table ACT_RU_EXECUTION add START_ACT_ID_ nvarchar(255);

create table ACT_RU_HISTORY_JOB (
    ID_ nvarchar(64) NOT NULL,
    REV_ int,
    LOCK_EXP_TIME_ datetime NULL,
    LOCK_OWNER_ nvarchar(255),
    RETRIES_ int,
    EXCEPTION_STACK_ID_ nvarchar(64),
    EXCEPTION_MSG_ nvarchar(4000),
    HANDLER_TYPE_ nvarchar(255),
    HANDLER_CFG_ nvarchar(4000),
    ADV_HANDLER_CFG_ID_ nvarchar(64),
    TENANT_ID_ nvarchar(255) default '',
    CREATE_TIME_ datetime2 NULL,
    primary key (ID_)
);
