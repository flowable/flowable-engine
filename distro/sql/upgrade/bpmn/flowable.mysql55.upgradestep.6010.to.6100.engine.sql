update ACT_GE_PROPERTY set VALUE_ = '6.1.0.0' where NAME_ = 'schema.version';

alter table ACT_RU_EXECUTION add column START_ACT_ID_ varchar(255);

create table ACT_RU_HISTORY_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    LOCK_EXP_TIME_ timestamp NULL,
    LOCK_OWNER_ varchar(255),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    ADV_HANDLER_CFG_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    CREATE_TIME_ timestamp NULL,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

