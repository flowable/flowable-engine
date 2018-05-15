/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.dmn.engine.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.RuleEngineExecutor;
import org.flowable.dmn.engine.impl.el.ELExecutionContext;
import org.flowable.dmn.engine.impl.el.ELExecutionContextBuilder;
import org.flowable.dmn.engine.impl.el.ELExpressionExecutor;
import org.flowable.dmn.engine.impl.el.ExecutionVariableFactory;
import org.flowable.dmn.engine.impl.hitpolicy.AbstractHitPolicy;
import org.flowable.dmn.engine.impl.hitpolicy.ComposeDecisionResultBehavior;
import org.flowable.dmn.engine.impl.hitpolicy.ComposeRuleResultBehavior;
import org.flowable.dmn.engine.impl.hitpolicy.ContinueEvaluatingBehavior;
import org.flowable.dmn.engine.impl.hitpolicy.EvaluateRuleValidityBehavior;
import org.flowable.dmn.engine.impl.persistence.entity.HistoricDecisionExecutionEntity;
import org.flowable.dmn.engine.impl.persistence.entity.HistoricDecisionExecutionEntityManager;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.HitPolicy;
import org.flowable.dmn.model.LiteralExpression;
import org.flowable.dmn.model.RuleInputClauseContainer;
import org.flowable.dmn.model.RuleOutputClauseContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Yvo Swillens
 */
public class RuleEngineExecutorImpl implements RuleEngineExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleEngineExecutorImpl.class);

    protected Map<String, AbstractHitPolicy> hitPolicyBehaviors;
    protected ExpressionManager expressionManager;
    protected ObjectMapper objectMapper;

    public RuleEngineExecutorImpl(Map<String, AbstractHitPolicy> hitPolicyBehaviors, ExpressionManager expressionManager, ObjectMapper objectMapper) {
        this.hitPolicyBehaviors = hitPolicyBehaviors;
        this.expressionManager = expressionManager;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the given decision table and creates the outcome results
     *
     * @param decision            the DMN decision
     * @param executeDecisionInfo
     * @return updated execution variables map
     */
    @Override
    public DecisionExecutionAuditContainer execute(Decision decision, ExecuteDecisionInfo executeDecisionInfo) {

        if (decision == null) {
            throw new IllegalArgumentException("no decision provided");
        }

        if (decision.getExpression() == null || !(decision.getExpression() instanceof DecisionTable)) {
            throw new IllegalArgumentException("no decision table present in decision");
        }

        DecisionTable currentDecisionTable = (DecisionTable) decision.getExpression();

        // create execution context and audit trail
        ELExecutionContext executionContext = ELExecutionContextBuilder.build(decision, executeDecisionInfo.getVariables());

        try {
            sanityCheckDecisionTable(currentDecisionTable);

            // evaluate decision table
            evaluateDecisionTable(currentDecisionTable, executionContext);

        } catch (FlowableException fe) {
            LOGGER.error("decision table execution sanity check failed", fe);
            executionContext.getAuditContainer().setFailed();
            executionContext.getAuditContainer().setExceptionMessage(getExceptionMessage(fe));

        } finally {
            // end audit trail
            executionContext.getAuditContainer().stopAudit();

            DmnEngineConfiguration dmnEngineConfiguration = CommandContextUtil.getDmnEngineConfiguration();
            if (dmnEngineConfiguration.isHistoryEnabled()) {
                HistoricDecisionExecutionEntityManager historicDecisionExecutionEntityManager = dmnEngineConfiguration.getHistoricDecisionExecutionEntityManager();
                HistoricDecisionExecutionEntity decisionExecutionEntity = historicDecisionExecutionEntityManager.create();
                decisionExecutionEntity.setDecisionDefinitionId(executeDecisionInfo.getDecisionDefinitionId());
                decisionExecutionEntity.setDeploymentId(executeDecisionInfo.getDeploymentId());
                decisionExecutionEntity.setStartTime(executionContext.getAuditContainer().getStartTime());
                decisionExecutionEntity.setEndTime(executionContext.getAuditContainer().getEndTime());
                decisionExecutionEntity.setInstanceId(executeDecisionInfo.getInstanceId());
                decisionExecutionEntity.setExecutionId(executeDecisionInfo.getExecutionId());
                decisionExecutionEntity.setActivityId(executeDecisionInfo.getActivityId());
                decisionExecutionEntity.setScopeType(executeDecisionInfo.getScopeType());
                decisionExecutionEntity.setTenantId(executeDecisionInfo.getTenantId());

                Boolean failed = executionContext.getAuditContainer().isFailed();
                if (BooleanUtils.isTrue(failed)) {
                    decisionExecutionEntity.setFailed(failed.booleanValue());
                }

                try {
                    decisionExecutionEntity.setExecutionJson(objectMapper.writeValueAsString(executionContext.getAuditContainer()));
                } catch (Exception e) {
                    throw new FlowableException("Error writing execution json", e);
                }

                historicDecisionExecutionEntityManager.insert(decisionExecutionEntity);
            }
        }

        return executionContext.getAuditContainer();
    }

    protected void evaluateDecisionTable(DecisionTable decisionTable, ELExecutionContext executionContext) {
        LOGGER.debug("Start table evaluation: {}", decisionTable.getId());

        if (decisionTable == null || decisionTable.getRules().isEmpty()) {
            throw new IllegalArgumentException("no rules present in table");
        }

        if (executionContext == null) {
            throw new FlowableException("no execution context available");
        }

        try {
            // evaluate rule conditions
            Map<Integer, List<RuleOutputClauseContainer>> validRuleOutputEntries = new HashMap<>();

            for (DecisionRule rule : decisionTable.getRules()) {
                boolean ruleResult = executeRule(rule, executionContext);

                if (ruleResult) {
                    // evaluate decision table hit policy validity
                    if (getHitPolicyBehavior(decisionTable.getHitPolicy()) instanceof EvaluateRuleValidityBehavior) {
                        ((EvaluateRuleValidityBehavior) getHitPolicyBehavior(decisionTable.getHitPolicy())).evaluateRuleValidity(rule.getRuleNumber(), executionContext);
                    }

                    // add valid rule output(s)
                    validRuleOutputEntries.put(rule.getRuleNumber(), rule.getOutputEntries());
                }

                // should continue evaluating
                if (getHitPolicyBehavior(decisionTable.getHitPolicy()) instanceof ContinueEvaluatingBehavior) {
                    if (getHitPolicyBehavior(decisionTable.getHitPolicy()).shouldContinueEvaluating(ruleResult) == false) {
                        LOGGER.debug("Stopping execution; hit policy {} specific behaviour", decisionTable.getHitPolicy());
                        break;
                    }
                }
            }

            // compose rule conclusions
            for (Map.Entry<Integer, List<RuleOutputClauseContainer>> entry : validRuleOutputEntries.entrySet()) {
                executeOutputEntryAction(entry.getKey(), entry.getValue(), decisionTable.getHitPolicy(), executionContext);
            }

            // post rule conclusion actions
            if (getHitPolicyBehavior(decisionTable.getHitPolicy()) instanceof ComposeDecisionResultBehavior) {
                getHitPolicyBehavior(decisionTable.getHitPolicy()).composeDecisionResults(executionContext);
            }

        } catch (FlowableException ade) {
            LOGGER.error("decision table execution failed", ade);
            executionContext.getRuleResults().clear();
            executionContext.getAuditContainer().setFailed();
            executionContext.getAuditContainer().setExceptionMessage(getExceptionMessage(ade));
        }

        LOGGER.debug("End table evaluation: {}", decisionTable.getId());
    }

    protected boolean executeRule(DecisionRule rule, ELExecutionContext executionContext) {
        if (rule == null) {
            throw new FlowableException("rule cannot be null");
        }

        LOGGER.debug("Start rule {} evaluation", rule.getRuleNumber());

        // add audit entry
        executionContext.getAuditContainer().addRuleEntry(rule);

        boolean conditionResult = false;

        // go through conditions
        for (RuleInputClauseContainer conditionContainer : rule.getInputEntries()) {

            // resetting value
            String inputEntryId = conditionContainer.getInputEntry().getId();
            conditionResult = false;

            try {
                // if condition is empty condition or has dash symbol result is TRUE
                String inputEntryText = conditionContainer.getInputEntry().getText();
                if (StringUtils.isEmpty(inputEntryText) || "-".equals(inputEntryText)) {
                    conditionResult = true;
                } else {
                    conditionResult = executeInputExpressionEvaluation(conditionContainer, executionContext);
                }

                // add audit entry
                executionContext.getAuditContainer().addInputEntry(rule.getRuleNumber(), inputEntryId, conditionResult);

                LOGGER.debug("input entry {} ( {} {} ): {} ", inputEntryId,
                        conditionContainer.getInputClause().getInputExpression().getText(),
                        inputEntryText, conditionResult);

            } catch (FlowableException ade) {
                // add failed audit entry and rethrow
                executionContext.getAuditContainer().addInputEntry(rule.getRuleNumber(), inputEntryId, getExceptionMessage(ade), null);
                throw ade;

            } catch (Exception e) {
                // add failed audit entry and rethrow
                executionContext.getAuditContainer().addInputEntry(rule.getRuleNumber(), inputEntryId, getExceptionMessage(e), null);
                throw new FlowableException(getExceptionMessage(e), e);
            }

            // exit evaluation loop if a condition is evaluated false
            if (!conditionResult) {
                break;
            }
        }

        if (conditionResult) {
            // mark rule valid
            executionContext.getAuditContainer().markRuleValid(rule.getRuleNumber());
        }

        // mark rule end
        executionContext.getAuditContainer().markRuleEnd(rule.getRuleNumber());

        LOGGER.debug("End rule {} evaluation", rule.getRuleNumber());
        return conditionResult;
    }

    protected Boolean executeInputExpressionEvaluation(RuleInputClauseContainer ruleContainer, ELExecutionContext executionContext) {
        return ELExpressionExecutor.executeInputExpression(ruleContainer.getInputClause(), ruleContainer.getInputEntry(), expressionManager, executionContext);
    }

    protected void executeOutputEntryAction(int ruleNumber, List<RuleOutputClauseContainer> ruleOutputContainers, HitPolicy hitPolicy, ELExecutionContext executionContext) {
        LOGGER.debug("Start conclusion processing");

        for (RuleOutputClauseContainer clauseContainer : ruleOutputContainers) {
            composeOutputEntryResult(ruleNumber, clauseContainer, hitPolicy, executionContext);
        }

        LOGGER.debug("End conclusion processing");
    }

    protected void composeOutputEntryResult(int ruleNumber, RuleOutputClauseContainer ruleClauseContainer, HitPolicy hitPolicy, ELExecutionContext executionContext) {
        LOGGER.debug("Start evaluation conclusion {} of valid rule {}", ruleClauseContainer.getOutputClause().getOutputNumber(), ruleNumber);

        String outputVariableId = ruleClauseContainer.getOutputClause().getName();
        String outputVariableType = ruleClauseContainer.getOutputClause().getTypeRef();

        LiteralExpression outputEntryExpression = ruleClauseContainer.getOutputEntry();

        if (StringUtils.isNotEmpty(outputEntryExpression.getText())) {
            Object executionVariable = null;
            try {
                Object resultValue = ELExpressionExecutor.executeOutputExpression(ruleClauseContainer.getOutputClause(), outputEntryExpression, expressionManager, executionContext);
                executionVariable = ExecutionVariableFactory.getExecutionVariable(outputVariableType, resultValue);

                // update execution context
                executionContext.getStackVariables().put(outputVariableId, executionVariable);

                // create result
                if (getHitPolicyBehavior(hitPolicy) instanceof ComposeRuleResultBehavior) {
                    ((ComposeRuleResultBehavior) getHitPolicyBehavior(hitPolicy)).composeRuleResult(ruleNumber, outputVariableId, executionVariable, executionContext);
                }

                // add audit entry
                executionContext.getAuditContainer().addOutputEntry(ruleNumber, outputEntryExpression.getId(), executionVariable);
                executionContext.getAuditContainer().addDecisionResultType(outputVariableId, outputVariableType);

                if (executionVariable != null) {
                    LOGGER.debug("Created conclusion result: {} of type: {} with value {} ", outputVariableId, resultValue.getClass(), resultValue.toString());
                } else {
                    LOGGER.warn("Could not create conclusion result");
                }

            } catch (FlowableException ade) {
                // clear result variables
                executionContext.getRuleResults().clear();

                // add failed audit entry and rethrow
                executionContext.getAuditContainer().addOutputEntry(ruleNumber, outputEntryExpression.getId(), getExceptionMessage(ade), executionVariable);
                throw ade;

            } catch (Exception e) {
                // clear result variables
                executionContext.getRuleResults().clear();

                // add failed audit entry and rethrow
                executionContext.getAuditContainer().addOutputEntry(ruleNumber, outputEntryExpression.getId(), getExceptionMessage(e), executionVariable);
                throw new FlowableException(getExceptionMessage(e), e);
            }

        } else {
            LOGGER.debug("Expression is empty");

            // add empty audit entry
            executionContext.getAuditContainer().addOutputEntry(ruleNumber, outputEntryExpression.getId(), null);
        }

        LOGGER.debug("End evaluation conclusion {} of valid rule {}", ruleClauseContainer.getOutputClause().getOutputNumber(), ruleNumber);
    }

    protected String getExceptionMessage(Exception exception) {
        String exceptionMessage;
        if (exception.getCause() != null && exception.getCause().getMessage() != null) {
            exceptionMessage = exception.getCause().getMessage();
        } else {
            exceptionMessage = exception.getMessage();
        }
        return exceptionMessage;
    }

    protected AbstractHitPolicy getHitPolicyBehavior(HitPolicy hitPolicy) {
        AbstractHitPolicy hitPolicyBehavior = hitPolicyBehaviors.get(hitPolicy.getValue());

        if (hitPolicyBehavior == null) {
            String hitPolicyBehaviorNotFoundMessage = String.format("HitPolicy behavior: %s not configured", hitPolicy.getValue());

            LOGGER.error(hitPolicyBehaviorNotFoundMessage);

            throw new FlowableException(hitPolicyBehaviorNotFoundMessage);
        }

        return hitPolicyBehavior;
    }

    protected void sanityCheckDecisionTable(DecisionTable decisionTable) {
        if (decisionTable.getHitPolicy() == HitPolicy.COLLECT && decisionTable.getAggregation() != null && decisionTable.getOutputs() != null) {
            if (decisionTable.getOutputs().size() > 1) {
                throw new FlowableException(String.format("HitPolicy: %s has aggregation: %s and multiple outputs. This is not supported", decisionTable.getHitPolicy(), decisionTable.getAggregation()));
            }
            if (!"number".equals(decisionTable.getOutputs().get(0).getTypeRef())) {
                throw new FlowableException(String.format("HitPolicy: %s has aggregation: %s needs output type number", decisionTable.getHitPolicy(), decisionTable.getAggregation()));
            }
        }
    }

	public Map<String, AbstractHitPolicy> getHitPolicyBehaviors() {
		return hitPolicyBehaviors;
	}

	public void setHitPolicyBehaviors(Map<String, AbstractHitPolicy> hitPolicyBehaviors) {
		this.hitPolicyBehaviors = hitPolicyBehaviors;
	}

	public ExpressionManager getExpressionManager() {
		return expressionManager;
	}

	public void setExpressionManager(ExpressionManager expressionManager) {
		this.expressionManager = expressionManager;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
}
