create database flowable
collate sql_latin1_general_cp1_cs_as
go
use flowable
go
CREATE LOGIN flowable WITH PASSWORD = N'flowable', CHECK_POLICY = OFF, CHECK_EXPIRATION = OFF;
go
CREATE USER [flowable] FOR LOGIN [flowable] EXEC sp_addrolemember N'db_owner', N'flowable'
go
