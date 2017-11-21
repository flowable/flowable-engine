package org.flowable.cmmn.engine.impl.behavior.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.impl.interceptor.CommandContext;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author martin.grofcik
 */
public class PlanItemDmnActivityBehavior extends CoreCmmnActivityBehavior {

        protected static final String EXPRESSION_DECISION_TABLE_REFERENCE_KEY = "decisionTableReferenceKey";

        protected ServiceTask task;

    public PlanItemDmnActivityBehavior(ServiceTask task) {
        this.task = task;
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        String referenceKeyValue = getReferenceKey(commandContext, planItemInstanceEntity);

        DmnRuleService dmnRuleService = CommandContextUtil.getDmnRuleService(commandContext);
        CaseDefinition caseDefinition = CaseDefinitionUtil.getCaseDefinition(planItemInstanceEntity.getCaseDefinitionId());
        setVariables(
                dmnRuleService.createExecuteDecisionBuilder().
                        decisionKey(referenceKeyValue).
                        parentDeploymentId(caseDefinition.getDeploymentId()).
                        instanceId(planItemInstanceEntity.getCaseInstanceId()).
                        executionId(planItemInstanceEntity.getId()).
                        activityId(task.getId()).
                        variables(planItemInstanceEntity.getVariables()).
                        tenantId(planItemInstanceEntity.getTenantId()).
                        execute(),
                referenceKeyValue,
                planItemInstanceEntity,
                CommandContextUtil.getCmmnEngineConfiguration(commandContext).getObjectMapper()
        );

        CommandContextUtil.getAgenda().planCompletePlanItemInstance(planItemInstanceEntity);
    }

    protected void setVariables(List<Map<String, Object>> executionResult,
                                String decisionKey,
                                PlanItemInstanceEntity planItemInstanceEntity,
                                ObjectMapper objectMapper) {
        if (executionResult == null || executionResult.isEmpty()) {
            return;
        }

        // multiple rule results
        // put on execution as JSON array; each entry contains output id (key) and output value (value)
        if (executionResult.size() > 1) {
            ArrayNode ruleResultNode = objectMapper.createArrayNode();

            for (Map<String, Object> ruleResult : executionResult) {
                ObjectNode outputResultNode = objectMapper.createObjectNode();

                for (Map.Entry<String, Object> outputResult : ruleResult.entrySet()) {
                    outputResultNode.set(outputResult.getKey(), objectMapper.convertValue(outputResult.getValue(), JsonNode.class));
                }

                ruleResultNode.add(outputResultNode);
            }

            planItemInstanceEntity.setVariable(decisionKey, ruleResultNode);
        } else {
            // single rule result
            // put on execution output id (key) and output value (value)
            Map<String, Object> ruleResult = executionResult.get(0);

            for (Map.Entry<String, Object> outputResult : ruleResult.entrySet()) {
                planItemInstanceEntity.setVariable(outputResult.getKey(), outputResult.getValue());
            }
        }
    }

    protected String getReferenceKey(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        Object referenceKeyValue;
        try {
            Expression referenceKeyExpression = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getExpressionManager().createExpression(
                    getReferenceKeyExpressionString()
            );
            referenceKeyValue = referenceKeyExpression.getValue(planItemInstanceEntity);
        } catch (Exception exc) {
            throw new FlowableException(exc.getMessage(), exc);
        }
        if (referenceKeyValue == null) {
            throw new FlowableIllegalArgumentException("Reference key expression must not be resolved to null " + planItemInstanceEntity);
        }
        if (!(referenceKeyValue instanceof String)) {
            throw new FlowableIllegalArgumentException("Reference key expression must be resolved to String. " + referenceKeyValue);
        }
        return (String) referenceKeyValue;
    }

    private String getReferenceKeyExpressionString() {
        Iterator<FieldExtension> iterator = this.task.getFieldExtensions().iterator();
        while (iterator.hasNext()) {
            FieldExtension fieldExtension = iterator.next();
            if (EXPRESSION_DECISION_TABLE_REFERENCE_KEY.equals(fieldExtension.getFieldName())) {
                return fieldExtension.getStringValue();
            }
        }
        return null;
    }
}
