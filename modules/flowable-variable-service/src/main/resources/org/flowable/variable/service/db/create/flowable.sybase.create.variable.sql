create table ACT_RU_VARIABLE (
    ID_ varchar(64) not null,
    REV_ int null,
    TYPE_ varchar(255) not null,
    NAME_ varchar(255) not null,
    EXECUTION_ID_ varchar(64) null,
    PROC_INST_ID_ varchar(64) null,
    TASK_ID_ varchar(64) null,
    SCOPE_ID_ varchar(255) null,
    SUB_SCOPE_ID_ varchar(255) null,
    SCOPE_TYPE_ varchar(255) null,
    BYTEARRAY_ID_ varchar(64) null,
    DOUBLE_ double precision null,
    LONG_ numeric(19,0) null,
    TEXT_ varchar(4000) null,
    TEXT2_ varchar(4000) null,
    primary key (ID_)
);

create index ACT_IDX_RU_VAR_SCOPE_ID_TYPE on ACT_RU_VARIABLE(SCOPE_ID_, SCOPE_TYPE_);
create index ACT_IDX_RU_VAR_SUB_ID_TYPE on ACT_RU_VARIABLE(SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_VARIABLE_BA on ACT_RU_VARIABLE(BYTEARRAY_ID_);
alter table ACT_RU_VARIABLE 
    add constraint ACT_FK_VAR_BYTEARRAY 
    foreign key (BYTEARRAY_ID_) 
    references ACT_GE_BYTEARRAY (ID_);

insert into ACT_GE_PROPERTY values ('variable.schema.version', '6.3.0.0', 1);