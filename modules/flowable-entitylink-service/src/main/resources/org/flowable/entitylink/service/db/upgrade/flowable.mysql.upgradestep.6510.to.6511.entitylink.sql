alter table ACT_RU_ENTITYLINK add column ROOT_SCOPE_ID_ varchar(255);
alter table ACT_RU_ENTITYLINK add column ROOT_SCOPE_TYPE_ varchar(255);
create index ACT_IDX_ENT_LNK_ROOT_SCOPE on ACT_RU_ENTITYLINK(ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_);

update ACT_GE_PROPERTY set VALUE_ = '6.5.1.1' where NAME_ = 'entitylink.schema.version';
