update ACT_GE_PROPERTY set VALUE_ = '7.1.0.1' where NAME_ = 'common.schema.version';

delete from ACT_GE_PROPERTY where NAME_ = 'batch.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'entitylink.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'eventsubscription.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'identitylink.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'job.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'task.schema.version';
delete from ACT_GE_PROPERTY where NAME_ = 'variable.schema.version';
