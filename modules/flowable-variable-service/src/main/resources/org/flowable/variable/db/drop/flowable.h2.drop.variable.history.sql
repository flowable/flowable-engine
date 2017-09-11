drop table if exists ACT_HI_VARINST cascade constraints;
drop table if exists ACT_HI_DETAIL cascade constraints;

drop index if exists ACT_IDX_HI_DETAIL_TIME;
drop index if exists ACT_IDX_HI_DETAIL_NAME;
drop index if exists ACT_IDX_HI_PROCVAR_NAME_TYPE;
