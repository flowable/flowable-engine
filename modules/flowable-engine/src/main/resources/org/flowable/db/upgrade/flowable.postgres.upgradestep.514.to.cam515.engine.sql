alter table ACT_RU_TASK 
    add CATEGORY_ varchar(255);
    
alter table ACT_RU_EXECUTION drop constraint ACT_FK_EXE_PROCDEF;

alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PROCDEF 
    foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_);

alter table ACT_RU_EVENT_SUBSCR
   add PROC_DEF_ID_ varchar(64);
    
alter table ACT_RE_PROCDEF
    add constraint ACT_UNIQ_PROCDEF
    unique (KEY_,VERSION_, TENANT_ID_);
    
alter table ACT_RU_IDENTITYLINK
    add PROC_INST_ID_ varchar(64);
    
alter table ACT_HI_IDENTITYLINK
    add PROC_INST_ID_ varchar(64);

alter table ACT_HI_IDENTITYLINK alter column TIMESTAMP_ drop not null;

update ACT_GE_PROPERTY set VALUE_ = '5.15' where NAME_ = 'schema.version';
