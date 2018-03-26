alter table ACT_RU_TASK add SUB_TASK_COUNT_ int;
update ACT_RU_TASK set SUB_TASK_COUNT_ = (select count(*) from (select * from ACT_RU_TASK where IS_COUNT_ENABLED_ = 1) as count_table where count_table.PARENT_TASK_ID_ = ID_) where IS_COUNT_ENABLED_ = 1;

update ACT_GE_PROPERTY set VALUE_ = '6.3.0.0' where NAME_ = 'task.schema.version';
