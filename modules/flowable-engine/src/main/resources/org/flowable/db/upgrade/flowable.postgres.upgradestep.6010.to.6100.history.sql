create index ACT_IDX_HI_PROCVAR_EXE on ACT_HI_VARINST(EXECUTION_ID_);

alter table ACT_HI_PROCINST add column REV_ integer default 1;
alter table ACT_HI_ACTINST add column REV_ integer default 1;
alter table ACT_HI_TASKINST add column REV_ integer default 1;
alter table ACT_HI_TASKINST add column LAST_UPDATED_TIME_ timestamp;
alter table ACT_HI_VARINST alter column REV_ set default 1;
