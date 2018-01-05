if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_ID_MEMBERSHIP') alter table ACT_ID_MEMBERSHIP drop constraint ACT_FK_MEMB_GROUP;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_ID_MEMBERSHIP') alter table ACT_ID_MEMBERSHIP drop constraint ACT_FK_MEMB_USER;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_ID_PRIV_MAPPING') alter table ACT_ID_PRIV_MAPPING drop constraint ACT_FK_PRIV_MAPPING;

if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_ID_PROPERTY') drop table ACT_ID_PROPERTY;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_ID_BYTEARRAY') drop table ACT_ID_BYTEARRAY;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_ID_INFO') drop table ACT_ID_INFO;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_ID_MEMBERSHIP') drop table ACT_ID_MEMBERSHIP;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_ID_GROUP') drop table ACT_ID_GROUP;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_ID_USER') drop table ACT_ID_USER;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_ID_TOKEN') drop table ACT_ID_TOKEN;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_ID_PRIV') drop table ACT_ID_PRIV;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_ID_PRIV_MAPPING') drop table ACT_ID_PRIV_MAPPING;


IF EXISTS (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_EXEC_BUSKEY') drop index ACT_RU_EXECUTION.ACT_IDX_EXEC_BUSKEY;
IF EXISTS (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_TASK_CREATE') drop index ACT_RU_TASK.ACT_IDX_TASK_CREATE;
IF EXISTS (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_IDENT_LNK_USER') drop index ACT_RU_IDENTITYLINK.ACT_IDX_IDENT_LNK_USER;
IF EXISTS (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_IDENT_LNK_GROUP') drop index ACT_RU_IDENTITYLINK.ACT_IDX_IDENT_LNK_GROUP;
IF EXISTS (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_VARIABLE_TASK_ID') drop index ACT_RU_VARIABLE.ACT_IDX_VARIABLE_TASK_ID;
IF EXISTS (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_EVENT_SUBSCR_CONFIG_') drop index ACT_RU_EVENT_SUBSCR.ACT_IDX_EVENT_SUBSCR_CONFIG_;
IF EXISTS (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_INFO_PROCDEF') drop index ACT_PROCDEF_INFO.ACT_IDX_INFO_PROCDEF;
