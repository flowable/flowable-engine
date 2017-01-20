alter table ACT_ID_MEMBERSHIP 
    drop foreign key ACT_FK_MEMB_GROUP;
    
alter table ACT_ID_MEMBERSHIP 
    drop foreign key ACT_FK_MEMB_USER;
    
alter table ACT_ID_PRIV_MAPPING
    drop foreign key ACT_FK_PRIV_MAPPING;    

drop table ACT_ID_PROPERTY;
drop table ACT_ID_BYTEARRAY;
drop table ACT_ID_INFO;
drop table ACT_ID_MEMBERSHIP;
drop table ACT_ID_GROUP;
drop table ACT_ID_USER;
drop table ACT_ID_TOKEN;
drop table ACT_ID_PRIV;
drop table ACT_ID_PRIV_MAPPING;
