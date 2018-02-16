insert into ACT_ID_PROPERTY
values ('schema.version', '6.3.0.0', 1);

alter table ACT_ID_USER add
    TENANT_ID_ NVARCHAR2(255) default '';

