alter table ACT_RE_DEPLOYMENT add DERIVED_FROM_ varchar(64);
alter table ACT_RE_DEPLOYMENT add DERIVED_FROM_ROOT_ varchar(64);
alter table ACT_RE_PROCDEF add DERIVED_FROM_ varchar(64);
alter table ACT_RE_PROCDEF add DERIVED_FROM_ROOT_ varchar(64);

update ACT_GE_PROPERTY set VALUE_ = '6.1.0.0' where NAME_ = 'schema.version';
