create index ACT_IDX_HI_PROCVAR_EXE on ACT_HI_VARINST(EXECUTION_ID_);

alter table ACT_HI_PROCINST add REV_ INTEGER default 1;
update ACT_HI_PROCINST set REV_ = 1;

alter table ACT_HI_ACTINST add REV_ INTEGER default 1;
update ACT_HI_ACTINST set REV_ = 1;

alter table ACT_HI_TASKINST add REV_ INTEGER default 1;
update ACT_HI_TASKINST set REV_ = 1;

alter table ACT_HI_TASKINST add LAST_UPDATED_TIME_ TIMESTAMP(6);
alter table ACT_HI_VARINST modify REV_ default 1;
update ACT_HI_VARINST set REV_ = 1 where REV_ is null OR REV_ = 0;
