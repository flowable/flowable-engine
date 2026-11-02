update ACT_GE_PROPERTY set VALUE_ = '6.5.0.5' where NAME_ = 'common.schema.version';


alter table ACT_RU_TASK add PROPAGATED_STAGE_INST_ID_ nvarchar(255);

alter table ACT_HI_TASKINST add PROPAGATED_STAGE_INST_ID_ nvarchar(255);

