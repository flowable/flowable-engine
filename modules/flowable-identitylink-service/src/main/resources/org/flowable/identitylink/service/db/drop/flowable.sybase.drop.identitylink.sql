IF EXISTS (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_IDENT_LNK_USER') drop index ACT_RU_IDENTITYLINK.ACT_IDX_IDENT_LNK_USER;
IF EXISTS (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_IDENT_LNK_GROUP') drop index ACT_RU_IDENTITYLINK.ACT_IDX_IDENT_LNK_GROUP;

if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_RU_IDENTITYLINK') drop table ACT_RU_IDENTITYLINK;