drop table if exists ACT_ID_PROPERTY cascade constraints;
drop table if exists ACT_ID_BYTEARRAY cascade constraints;
drop table if exists ACT_ID_INFO cascade constraints;
drop table if exists ACT_ID_GROUP cascade constraints;
drop table if exists ACT_ID_MEMBERSHIP cascade constraints;
drop table if exists ACT_ID_USER cascade constraints;
drop table if exists ACT_ID_TOKEN cascade constraints;
drop table if exists ACT_ID_PRIV cascade constraints;
drop table if exists ACT_ID_PRIV_MAPPING cascade constraints;

drop index if exists ACT_IDX_PRIV_USER;
drop index if exists ACT_IDX_PRIV_GROUP;