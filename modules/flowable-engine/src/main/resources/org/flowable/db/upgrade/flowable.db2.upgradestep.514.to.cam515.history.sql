alter table ACT_HI_TASKINST
    add CATEGORY_ varchar(255);
    
alter table ACT_HI_TASKINST
    add FORM_KEY_ varchar(255);
    
Call Sysproc.admin_cmd ('REORG TABLE ACT_HI_TASKINST');    

alter table ACT_HI_TASKINST add CLAIM_TIME_ timestamp;
    
alter table ACT_HI_VARINST
    add LAST_UPDATED_TIME_ timestamp; 

-- DB2 *cannot* drop columns. Yes, this is 2013.
-- This means that for DB2 the columns will remain as they are (they won't be used)
-- alter table ACT_HI_PROCINST drop colum UNI_BUSINESS_KEY;
-- alter table ACT_HI_PROCINST drop colum UNI_PROC_DEF_ID;
-- Call Sysproc.admin_cmd ('REORG TABLE ACT_HI_PROCINST');       

alter table ACT_HI_ACTINST alter column ASSIGNEE_ SET DATA TYPE varchar(255);

Call Sysproc.admin_cmd ('REORG TABLE ACT_HI_ACTINST');