alter table ACT_RE_PROCDEF add column ENGINE_VERSION_ varchar(255);
update ACT_RE_PROCDEF set ENGINE_VERSION_ = 'v5';

alter table ACT_RE_DEPLOYMENT add column ENGINE_VERSION_ varchar(255);
update ACT_RE_DEPLOYMENT set ENGINE_VERSION_ = 'v5';;

update ACT_GE_PROPERTY set VALUE_ = '6.0.0.0' where NAME_ = 'schema.version';
