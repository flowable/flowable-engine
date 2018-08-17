package org.flowable.engine.impl.bpmn.helper;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MultiInstanceCompletionConditionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiInstanceCompletionConditionEvaluator.class);

    public boolean evaluateCompletionCondition(ExecutionEntity execution) {
        MultiInstanceLoopCharacteristics multiInstanceLoopCharacteristics = getMultiInstanceLoopCharacteristics(execution);
        if (multiInstanceLoopCharacteristics == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No multi instance loop characteristics detected for execution {}", execution);
            }
            return false;
        }

        if (multiInstanceLoopCharacteristics.getCompletionCondition() == null) {
            return false;
        }

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();

        String activeCompletionCondition = null;

        if (CommandContextUtil.getProcessEngineConfiguration()
            .isEnableProcessDefinitionInfoCache()) {
            ObjectNode taskElementProperties = BpmnOverrideContext.getBpmnOverrideElementProperties(execution.getActivityId(),
                execution.getProcessDefinitionId());
            activeCompletionCondition = getActiveValue(multiInstanceLoopCharacteristics.getCompletionCondition(),
                DynamicBpmnConstants.MULTI_INSTANCE_COMPLETION_CONDITION, taskElementProperties);

        } else {
            activeCompletionCondition = multiInstanceLoopCharacteristics.getCompletionCondition();
        }

        Object value = expressionManager.createExpression(activeCompletionCondition)
            .getValue(execution);

        if (!(value instanceof Boolean)) {
            throw new FlowableIllegalArgumentException(
                "completionCondition '" + activeCompletionCondition + "' does not evaluate to a boolean value");
        }

        Boolean booleanValue = (Boolean) value;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Completion condition of multi-instance satisfied: {}", booleanValue);
        }
        return booleanValue;
    }

    private MultiInstanceLoopCharacteristics getMultiInstanceLoopCharacteristics(ExecutionEntity execution) {
        FlowElement currentFlowElement = execution.getCurrentFlowElement();
        return ((Activity) currentFlowElement).getLoopCharacteristics();
    }

    protected String getActiveValue(String originalValue, String propertyName, ObjectNode taskElementProperties) {
        String activeValue = originalValue;
        if (taskElementProperties != null) {
            JsonNode overrideValueNode = taskElementProperties.get(propertyName);
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
