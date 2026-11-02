update ACT_GE_PROPERTY set VALUE_ = '6.3.0.1' where NAME_ = 'common.schema.version';


alter table ACT_RU_TASK add TASK_DEF_ID_ NVARCHAR2(64);

alter table ACT_HI_TASKINST add TASK_DEF_ID_ NVARCHAR2(64);

