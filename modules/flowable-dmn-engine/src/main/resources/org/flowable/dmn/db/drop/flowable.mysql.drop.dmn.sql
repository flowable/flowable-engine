drop index ACT_IDX_DMN_INSTANCE_ID on ACT_DMN_HI_DECISION_EXECUTION;
drop index ACT_IDX_DMN_DEC_UNIQ on ACT_DMN_DECISION;

drop table if exists ACT_DMN_HI_DECISION_EXECUTION;
drop table if exists ACT_DMN_DECISION;
drop table if exists ACT_DMN_DEPLOYMENT_RESOURCE;
drop table if exists ACT_DMN_DEPLOYMENT;
