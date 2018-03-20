alter table ACT_RU_TASK add SUB_TASK_COUNT_ INTEGER;
update ACT_RU_TASK t set t.SUB_TASK_COUNT_ = (select count(*) from (select * from ACT_RU_TASK where IS_COUNT_ENABLED_ = 1) as count_table where PARENT_TASK_ID_ = t.ID_) where t.IS_COUNT_ENABLED_ = 1;
alter table ACT_RU_TASK add TASK_DEF_ID_ NVARCHAR2(64);

update ACT_GE_PROPERTY set VALUE_ = '6.3.0.0' where NAME_ = 'task.schema.version';
