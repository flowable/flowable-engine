alter table ACT_RE_DEPLOYMENT add DERIVED_FROM_ NVARCHAR2(64);
alter table ACT_RE_DEPLOYMENT add DERIVED_FROM_ROOT_ NVARCHAR2(64);
alter table ACT_RE_PROCDEF add DERIVED_FROM_ NVARCHAR2(64);
alter table ACT_RE_PROCDEF add DERIVED_FROM_ROOT_ NVARCHAR2(64);

alter table ACT_RE_PROCDEF add DERIVED_VERSION_ INTEGER DEFAULT 0 NOT NULL;

alter table ACT_RE_PROCDEF drop constraint ACT_UNIQ_PROCDEF;

begin
  execute immediate 'drop index ACT_UNIQ_PROCDEF';
exception
  when others then
    if sqlcode != -1418 then
      raise;
    end if;
end;
/
    
alter table ACT_RE_PROCDEF
    add constraint ACT_UNIQ_PROCDEF
    unique (KEY_,VERSION_, DERIVED_VERSION_, TENANT_ID_);

update ACT_GE_PROPERTY set VALUE_ = '6.3.0.1' where NAME_ = 'schema.version';
