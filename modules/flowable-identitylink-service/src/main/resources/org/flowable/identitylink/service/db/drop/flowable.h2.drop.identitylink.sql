drop table if exists ACT_RU_IDENTITYLINK cascade constraints;

drop index if exists ACT_IDX_IDENT_LNK_USER;
drop index if exists ACT_IDX_IDENT_LNK_GROUP;
drop index if exists ACT_IDX_IDENT_LNK_SCOPE;
drop index if exists ACT_IDX_IDENT_LNK_SUB_SCOPE;
drop index if exists ACT_IDX_IDENT_LNK_SCOPE_DEF;