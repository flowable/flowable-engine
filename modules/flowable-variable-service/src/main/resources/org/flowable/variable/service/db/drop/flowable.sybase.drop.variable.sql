if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_RU_VARIABLE') alter table ACT_RU_VARIABLE drop constraint ACT_FK_VAR_BYTEARRAY;
IF EXISTS (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_RU_VAR_SCOPE_ID_TYPE') drop index ACT_RU_VARIABLE.ACT_IDX_RU_VAR_SCOPE_ID_TYPE;
IF EXISTS (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_RU_VAR_SUB_ID_TYPE') drop index ACT_RU_VARIABLE.ACT_IDX_RU_VAR_SUB_ID_TYPE;

if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_RU_VARIABLE') drop table ACT_RU_VARIABLE;