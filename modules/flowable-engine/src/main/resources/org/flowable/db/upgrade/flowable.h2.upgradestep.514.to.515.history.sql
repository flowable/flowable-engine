alter table ACT_HI_TASKINST
    add CATEGORY_ varchar(191);
    
alter table ACT_HI_PROCINST drop constraint ACT_UNIQ_HI_BUS_KEY;    
    
alter table ACT_HI_VARINST
    add CREATE_TIME_ timestamp; 
    
alter table ACT_HI_VARINST
    add LAST_UPDATED_TIME_ timestamp; 
    
alter table ACT_HI_PROCINST
    add TENANT_ID_ varchar(191) default ''; 
       
alter table ACT_HI_ACTINST
    add TENANT_ID_ varchar(191) default ''; 
    
alter table ACT_HI_TASKINST
    add TENANT_ID_ varchar(191) default '';    
    
alter table ACT_HI_ACTINST
    alter column ASSIGNEE_ varchar(191);