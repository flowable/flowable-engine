update ACT_GE_PROPERTY set VALUE_ = '6.5.0.5' where NAME_ = 'common.schema.version';


alter table ACT_RU_TASK add column PROPAGATED_STAGE_INST_ID_ varchar(255);

alter table ACT_HI_TASKINST add column PROPAGATED_STAGE_INST_ID_ varchar(255);
