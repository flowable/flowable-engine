update ACT_GE_PROPERTY set VALUE_ = '6.7.2.1' where NAME_ = 'common.schema.version';


create index ACT_IDX_EVENT_SUBSCR_SCOPEREF_ on ACT_RU_EVENT_SUBSCR(SCOPE_ID_, SCOPE_TYPE_);

