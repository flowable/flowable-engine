alter table ACT_ID_USER 
add PICTURE_ID_ nvarchar(64);

create table ACT_ID_INFO (
    ID_ nvarchar(64),
    REV_ integer,
    USER_ID_ nvarchar(64),
    TYPE_ nvarchar(64),
    KEY_ nvarchar(191),
    VALUE_ nvarchar(191),
    PASSWORD_ image,
    PARENT_ID_ nvarchar(191),
    primary key (ID_)
);
