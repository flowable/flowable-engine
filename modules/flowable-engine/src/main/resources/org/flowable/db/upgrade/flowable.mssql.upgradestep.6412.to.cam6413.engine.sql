delete
from ACT_RU_VARIABLE
where TYPE_ in ('item', 'message')
  and BYTEARRAY_ID_ is null
  and DOUBLE_ is null
  and LONG_ is null
  and TEXT_ is null
  and TEXT2_ is null;

delete
from ACT_RU_VARIABLE
where TYPE_ = 'null'
  and NAME_ in ('org.activiti.engine.impl.bpmn.CURRENT_MESSAGE', 'org.flowable.engine.impl.bpmn.CURRENT_MESSAGE');

update ACT_GE_PROPERTY set VALUE_ = '6.4.1.3' where NAME_ = 'schema.version';
