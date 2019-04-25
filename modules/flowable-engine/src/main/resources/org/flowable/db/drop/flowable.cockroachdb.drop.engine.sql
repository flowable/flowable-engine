drop table if exists act_ru_actinst cascade;
drop table if exists act_re_deployment cascade;
drop table if exists act_re_model cascade;
drop table if exists act_re_procdef cascade;
drop table if exists act_ru_execution cascade;
drop table if exists act_evt_log cascade;
drop sequence if exists act_evt_log_nr__seq;
drop table if exists act_procdef_info cascade;

-- force-commit