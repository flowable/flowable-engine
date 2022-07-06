alter table ACT_HI_TASKINST
    add CATEGORY_ varchar(255);
    
alter table ACT_HI_TASKINST
  add FORM_KEY_ varchar(255);

alter table ACT_HI_TASKINST add CLAIM_TIME_ datetime(3);
    
alter table ACT_HI_VARINST
    add LAST_UPDATED_TIME_ datetime(3);   
    
alter table ACT_HI_ACTINST
    MODIFY ASSIGNEE_ varchar(255);

