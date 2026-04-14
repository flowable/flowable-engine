update ACT_GE_PROPERTY set VALUE_ = '8.0.0.0' where NAME_ = 'common.schema.version';


update ACT_GE_PROPERTY set VALUE_ = '8.0.0.0' where NAME_ = 'schema.version';


alter table ACT_HI_PROCINST add column END_USER_ID_ varchar(255);
alter table ACT_HI_PROCINST add column STATE_ varchar(255);

update ACT_GE_PROPERTY set VALUE_ = '8.0.0.0' where NAME_ = 'app.schema.version';


ALTER TABLE ACT_CMMN_HI_CASE_INST ADD END_USER_ID_ VARCHAR(255);

update ACT_GE_PROPERTY set VALUE_ = '8.0.0.0' where NAME_ = 'cmmn.schema.version';

update ACT_GE_PROPERTY set VALUE_ = '8.0.0.0' where NAME_ = 'dmn.schema.version';


update ACT_GE_PROPERTY set VALUE_ = '8.0.0.0' where NAME_ = 'eventregistry.schema.version';


update ACT_ID_PROPERTY set VALUE_ = '8.0.0.0' where NAME_ = 'schema.version';


