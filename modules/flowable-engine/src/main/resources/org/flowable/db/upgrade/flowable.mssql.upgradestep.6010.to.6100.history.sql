create index ACT_IDX_HI_PROCVAR_EXE on ACT_HI_VARINST(EXECUTION_ID_);

alter table ACT_HI_PROCINST add REV_ int default 1;
alter table ACT_HI_ACTINST add REV_ int default 1;
alter table ACT_HI_TASKINST add REV_ int default 1;
alter table ACT_HI_TASKINST add LAST_UPDATED_TIME_ datetime;
alter table ACT_HI_VARINST alter column REV_ default 1;
