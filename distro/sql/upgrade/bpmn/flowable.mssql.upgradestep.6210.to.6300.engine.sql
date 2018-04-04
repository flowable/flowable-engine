update ACT_GE_PROPERTY set VALUE_ = '6.3.0.0' where NAME_ = 'identitylink.schema.version';

alter table ACT_RU_IDENTITYLINK add SCOPE_ID_ nvarchar(255);
alter table ACT_RU_IDENTITYLINK add SCOPE_TYPE_ nvarchar(255);
alter table ACT_RU_IDENTITYLINK add SCOPE_DEFINITION_ID_ nvarchar(255);

create index ACT_IDX_IDENT_LNK_SCOPE on ACT_RU_IDENTITYLINK(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_IDENT_LNK_SCOPE_DEF on ACT_RU_IDENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

alter table ACT_RU_TASK add SUB_TASK_COUNT_ int;
update ACT_RU_TASK set SUB_TASK_COUNT_ = (select count(*) from (select * from ACT_RU_TASK where IS_COUNT_ENABLED_ = 1) as count_table where count_table.PARENT_TASK_ID_ = ID_) where IS_COUNT_ENABLED_ = 1;

update ACT_GE_PROPERTY set VALUE_ = '6.3.0.0' where NAME_ = 'task.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '6.3.0.0' where NAME_ = 'variable.schema.version';

update ACT_RU_TIMER_JOB set HANDLER_TYPE_ = 'cmmn-trigger-timer' where HANDLER_TYPE_ = 'trigger-timer' and SCOPE_TYPE_ = 'cmmn';

update ACT_GE_PROPERTY set VALUE_ = '6.3.0.0' where NAME_ = 'job.schema.version';
update ACT_GE_PROPERTY set VALUE_ = '6.3.0.0' where NAME_ = 'schema.version';

