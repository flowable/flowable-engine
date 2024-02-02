alter table ACT_RU_TASK 
    add CATEGORY_ nvarchar(255);
    
alter table ACT_RU_EVENT_SUBSCR
    add PROC_DEF_ID_ nvarchar(64);
    
drop index ACT_IDX_PROCDEF_TENANT_ID on ACT_RE_PROCDEF;
    
update ACT_RE_PROCDEF set TENANT_ID_ = '' where TENANT_ID_ is null;

alter table ACT_RE_PROCDEF alter column TENANT_ID_ nvarchar(64) not null;

alter table ACT_RE_PROCDEF add default('') for TENANT_ID_;
    
alter table ACT_RE_PROCDEF
    add constraint ACT_UNIQ_PROCDEF
    unique (KEY_,VERSION_, TENANT_ID_); 
    
drop index ACT_UNIQ_VARIABLE on ACT_RU_VARIABLE;

alter table ACT_RU_VARIABLE alter column VAR_SCOPE_ nvarchar(64) null;
    
alter table ACT_RU_IDENTITYLINK
    add PROC_INST_ID_ nvarchar(64);
    
alter table ACT_HI_IDENTITYLINK
    add PROC_INST_ID_ nvarchar(64);
    
drop index ACT_IDX_HI_IDENT_LNK_TIMESTAMP on ACT_HI_IDENTITYLINK;
    
alter table ACT_HI_IDENTITYLINK alter column TIMESTAMP_ datetime null;
    
update ACT_GE_PROPERTY set VALUE_ = '5.15' where NAME_ = 'schema.version';
