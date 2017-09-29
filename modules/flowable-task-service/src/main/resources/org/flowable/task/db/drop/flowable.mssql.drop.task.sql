if exists (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_TASK_CREATE') drop index ACT_RU_TASK.ACT_IDX_TASK_CREATE;

if exists (select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_NAME = 'ACT_RU_TASK') drop table ACT_RU_TASK;