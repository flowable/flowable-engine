alter table ACT_ID_USER 
add PICTURE_ID_ varchar(64);

create table ACT_ID_INFO (
    ID_ varchar(64),
    REV_ integer,
    USER_ID_ varchar(64),
    TYPE_ varchar(64),
    KEY_ varchar(191),
    VALUE_ varchar(191),
    PASSWORD_ longvarbinary,
    PARENT_ID_ varchar(191),
    primary key (ID_)
);
