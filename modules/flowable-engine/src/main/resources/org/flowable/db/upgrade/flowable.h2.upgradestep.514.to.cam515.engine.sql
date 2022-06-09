alter table ACT_RU_TASK 
    add CATEGORY_ varchar(255);

alter table ACT_RU_EVENT_SUBSCR
    add PROC_DEF_ID_ varchar(64);        
    
alter table ACT_RE_PROCDEF
    drop constraint ACT_UNIQ_PROCDEF;
    
alter table ACT_RE_PROCDEF
    add constraint ACT_UNIQ_PROCDEF
    unique (KEY_,VERSION_, TENANT_ID_);
    
alter table ACT_RU_IDENTITYLINK
    add PROC_INST_ID_ varchar(64);
    
alter table ACT_HI_IDENTITYLINK
    add PROC_INST_ID_ varchar(64);
    
alter table ACT_HI_IDENTITYLINK alter column CREATE_TIME_ timestamp null; 
    
update ACT_GE_PROPERTY set VALUE_ = '5.15' where NAME_ = 'schema.version';
