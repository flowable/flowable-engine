if exists (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_TASK_CREATE') drop index ACT_RU_TASK.ACT_IDX_TASK_CREATE;
if exists (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_TASK_SCOPE') drop index ACT_RU_TASK.ACT_IDX_TASK_SCOPE;
if exists (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_TASK_SUB_SCOPE') drop index ACT_RU_TASK.ACT_IDX_TASK_SUB_SCOPE;
if exists (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_TASK_SCOPE_DEF') drop index ACT_RU_TASK.ACT_IDX_TASK_SCOPE_DEF;
if exists (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_HI_TASK_LOG_NUMBER') drop index ACT_HI_TSK_LOG.ACT_IDX_HI_TASK_LOG_NUMBER;

if exists (select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_NAME = 'ACT_RU_TASK') drop table ACT_RU_TASK;
if exists (select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_NAME = 'ACT_HI_TSK_LOG') drop table ACT_HI_TSK_LOG;