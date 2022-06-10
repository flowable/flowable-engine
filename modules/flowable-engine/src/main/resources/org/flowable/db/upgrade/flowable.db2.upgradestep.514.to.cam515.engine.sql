alter table ACT_RU_TASK 
    add CATEGORY_ varchar(255);

Call Sysproc.admin_cmd ('REORG TABLE ACT_RU_TASK');

-- DB2 *cannot* drop columns. Yes, this is 2013.
-- This means that for DB2 the columns will remain as they are (they won't be used)
-- alter table ACT_RU_EXECUTION drop colum UNI_BUSINESS_KEY;
-- alter table ACT_RU_EXECUTION drop colum UNI_PROC_DEF_ID;

Call Sysproc.admin_cmd ('REORG TABLE ACT_RU_EXECUTION');
   
alter table ACT_RU_EVENT_SUBSCR
    add PROC_DEF_ID_ varchar(64);      
   
Call Sysproc.admin_cmd ('REORG TABLE ACT_RU_EVENT_SUBSCR');

update ACT_RE_PROCDEF set TENANT_ID_ = '' where TENANT_ID_ is null;

alter table ACT_RE_PROCDEF alter column TENANT_ID_ set not null;

alter table ACT_RE_PROCDEF alter column TENANT_ID_ set default '';

Call Sysproc.admin_cmd ('REORG TABLE ACT_RE_PROCDEF');
    
alter table ACT_RE_PROCDEF
    add constraint ACT_UNIQ_PROCDEF
    unique (KEY_,VERSION_, TENANT_ID_);  
    
Call Sysproc.admin_cmd ('REORG TABLE ACT_RE_PROCDEF');

drop index ACT_UNIQ_VARIABLE;

alter table ACT_RU_VARIABLE alter column VAR_SCOPE_ drop not null;

Call Sysproc.admin_cmd ('REORG TABLE ACT_RU_VARIABLE');

alter table ACT_RU_IDENTITYLINK
    add PROC_INST_ID_ varchar(64);
    
Call Sysproc.admin_cmd ('REORG TABLE ACT_RU_IDENTITYLINK');
    
alter table ACT_HI_IDENTITYLINK
    add PROC_INST_ID_ varchar(64);
    
alter table ACT_HI_IDENTITYLINK alter column TIMESTAMP_ drop not null;

Call Sysproc.admin_cmd ('REORG TABLE ACT_HI_IDENTITYLINK');

update ACT_GE_PROPERTY set VALUE_ = '5.15' where NAME_ = 'schema.version';
