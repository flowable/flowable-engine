if exists (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_TASK_CREATE') drop index ACT_RU_TASK.ACT_IDX_TASK_CREATE;
if exists (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_TASK_SCOPE') drop index ACT_RU_TASK.ACT_IDX_TASK_SCOPE;
if exists (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_TASK_SUB_SCOPE') drop index ACT_RU_TASK.ACT_IDX_TASK_SUB_SCOPE;
if exists (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_TASK_SCOPE_DEF') drop index ACT_RU_TASK.ACT_IDX_TASK_SCOPE_DEF;

if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_RU_TASK') drop table ACT_RU_TASK;