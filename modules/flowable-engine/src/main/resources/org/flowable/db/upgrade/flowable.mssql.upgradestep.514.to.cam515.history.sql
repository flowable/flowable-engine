alter table ACT_HI_TASKINST
    add CATEGORY_ nvarchar(255);
    
alter table ACT_HI_TASKINST
    add FORM_KEY_ nvarchar(255);
    
alter table ACT_HI_TASKINST add CLAIM_TIME_ datetime;
    
alter table ACT_HI_VARINST
    add LAST_UPDATED_TIME_ datetime;       
    
alter table ACT_HI_ACTINST
    alter column ASSIGNEE_ nvarchar(255);
    