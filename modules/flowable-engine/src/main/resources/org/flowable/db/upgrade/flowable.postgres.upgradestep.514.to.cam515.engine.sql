alter table ACT_RU_TASK 
    add CATEGORY_ varchar(255);

alter table ACT_RU_EVENT_SUBSCR
   add PROC_DEF_ID_ varchar(64);
   
update ACT_RE_PROCDEF set TENANT_ID_ = '' where TENANT_ID_ is null;

alter table ACT_RE_PROCDEF alter column TENANT_ID_ set not null;

alter table ACT_RE_PROCDEF alter column TENANT_ID_ set default '';
    
alter table ACT_RE_PROCDEF
    add constraint ACT_UNIQ_PROCDEF
    unique (KEY_,VERSION_, TENANT_ID_);
    
alter table ACT_RU_VARIABLE
    drop constraint ACT_UNIQ_VARIABLE;
    
alter table ACT_RU_IDENTITYLINK
    add PROC_INST_ID_ varchar(64);
    
alter table ACT_HI_IDENTITYLINK
    add PROC_INST_ID_ varchar(64);

alter table ACT_HI_IDENTITYLINK alter column TIMESTAMP_ drop not null;

update ACT_GE_PROPERTY set VALUE_ = '5.15' where NAME_ = 'schema.version';
