if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_RU_JOB') alter table ACT_RU_JOB drop constraint ACT_FK_JOB_EXCEPTION;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_RU_TIMER_JOB') alter table ACT_RU_TIMER_JOB drop constraint ACT_FK_TIMER_JOB_EXCEPTION;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_RU_SUSPENDED_JOB') alter table ACT_RU_SUSPENDED_JOB drop constraint ACT_FK_SUSPENDED_JOB_EXCEPTION;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_RU_DEADLETTER_JOB') alter table ACT_RU_DEADLETTER_JOB drop constraint ACT_FK_DEADLETTER_JOB_EXCEPTION;

if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_RU_JOB') drop table ACT_RU_JOB;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_RU_TIMER_JOB') drop table ACT_RU_TIMER_JOB;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_RU_SUSPENDED_JOB') drop table ACT_RU_SUSPENDED_JOB;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_RU_DEADLETTER_JOB') drop table ACT_RU_DEADLETTER_JOB;
if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_RU_HISTORY_JOB') drop table ACT_RU_HISTORY_JOB;