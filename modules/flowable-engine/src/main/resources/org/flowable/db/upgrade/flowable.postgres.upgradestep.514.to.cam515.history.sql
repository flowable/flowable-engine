alter table ACT_HI_TASKINST
    add CATEGORY_ varchar(255);
    
alter table ACT_HI_TASKINST
    add FORM_KEY_ varchar(255);

alter table ACT_HI_TASKINST add CLAIM_TIME_ timestamp;
    
alter table ACT_HI_VARINST
    add LAST_UPDATED_TIME_ timestamp;

alter table ACT_HI_ACTINST
	alter column ASSIGNEE_ TYPE varchar(255);