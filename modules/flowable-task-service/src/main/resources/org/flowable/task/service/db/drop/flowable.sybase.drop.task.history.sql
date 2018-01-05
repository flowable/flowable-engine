if exists (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_HI_TASK_SCOPE') drop index ACT_HI_TASKINST.ACT_IDX_HI_TASK_SCOPE;
if exists (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_HI_TASK_SUB_SCOPE') drop index ACT_HI_TASKINST.ACT_IDX_HI_TASK_SUB_SCOPE;
if exists (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_HI_TASK_SCOPE_DEF') drop index ACT_HI_TASKINST.ACT_IDX_HI_TASK_SCOPE_DEF;

if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_HI_TASKINST') drop table ACT_HI_TASKINST;