IF EXISTS (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_HI_IDENT_LNK_USER') drop index ACT_HI_IDENTITYLINK.ACT_IDX_HI_IDENT_LNK_USER;

if exists (select convert(varchar(30),o.name) AS table_name from sysobjects o where type = 'U' and o.name = 'ACT_HI_IDENTITYLINK') drop table ACT_HI_IDENTITYLINK;