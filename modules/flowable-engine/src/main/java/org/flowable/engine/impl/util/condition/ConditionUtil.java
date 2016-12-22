package org.flowable.engine.impl.util.condition;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.Expression;
import org.flowable.engine.impl.Condition;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.el.UelExpressionCondition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class ConditionUtil {

  public static boolean hasTrueCondition(SequenceFlow sequenceFlow, DelegateExecution execution) {
    String conditionExpression = null;
    if (Context.getProcessEngineConfiguration().isEnableProcessDefinitionInfoCache()) {
      ObjectNode elementProperties = Context.getBpmnOverrideElementProperties(sequenceFlow.getId(), execution.getProcessDefinitionId());
      conditionExpression = getActiveValue(sequenceFlow.getConditionExpression(), DynamicBpmnConstants.SEQUENCE_FLOW_CONDITION, elementProperties);
    } else {
      conditionExpression = sequenceFlow.getConditionExpression();
    }
    
    if (StringUtils.isNotEmpty(conditionExpression)) {

      Expression expression = Context.getProcessEngineConfiguration().getExpressionManager().createExpression(conditionExpression);
      Condition condition = new UelExpressionCondition(expression);
      return condition.evaluate(sequenceFlow.getId(), execution);
    } else {
      return true;
    }

  }
    
  protected static String getActiveValue(String originalValue, String propertyName, ObjectNode elementProperties) {
    String activeValue = originalValue;
    if (elementProperties != null) {
      JsonNode overrideValueNode = elementProperties.get(propertyName);
      if (overrideValueNode != null) {
        if (overrideValueNode.isNull()) {
          activeValue = null;
        } else {
          activeValue = overrideValueNode.asText();
        }
      }
    }
    return activeValue;
  }

}
