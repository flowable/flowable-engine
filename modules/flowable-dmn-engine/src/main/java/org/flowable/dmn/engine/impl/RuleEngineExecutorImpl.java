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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.dmn.api.RuleEngineExecutionResult;
import org.flowable.dmn.engine.FlowableDmnExpressionException;
import org.flowable.dmn.engine.RuleEngineExecutor;
import org.flowable.dmn.engine.impl.hitpolicy.AbstractHitPolicy;
import org.flowable.dmn.engine.impl.hitpolicy.ComposeDecisionResultBehavior;
import org.flowable.dmn.engine.impl.hitpolicy.ComposeRuleResultBehavior;
import org.flowable.dmn.engine.impl.hitpolicy.ContinueEvaluatingBehavior;
import org.flowable.dmn.engine.impl.hitpolicy.EvaluateRuleValidityBehavior;
import org.flowable.dmn.engine.impl.mvel.ExecutionVariableFactory;
import org.flowable.dmn.engine.impl.mvel.MvelExecutionContext;
import org.flowable.dmn.engine.impl.mvel.MvelExecutionContextBuilder;
import org.flowable.dmn.engine.impl.mvel.MvelExpressionExecutor;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.HitPolicy;
import org.flowable.dmn.model.LiteralExpression;
import org.flowable.dmn.model.RuleInputClauseContainer;
import org.flowable.dmn.model.RuleOutputClauseContainer;
import org.flowable.engine.common.api.FlowableException;
import org.mvel2.integration.PropertyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 */
public class RuleEngineExecutorImpl implements RuleEngineExecutor {

    private static final Logger logger = LoggerFactory.getLogger(RuleEngineExecutorImpl.class);

    protected Map<String, AbstractHitPolicy> hitPolicyBehaviors;

    public RuleEngineExecutorImpl(Map<String, AbstractHitPolicy> hitPolicyBehaviors) {
        this.hitPolicyBehaviors = hitPolicyBehaviors;
    }

    /**
     * Executes the given decision table and creates the outcome results
     *
     * @param decision       the DMN decision
     * @param inputVariables map with input variables
     * @return updated execution variables map
     */
    @Override
    public RuleEngineExecutionResult execute(Decision decision, Map<String, Object> inputVariables,
                                             Map<String, Method> customExpressionFunctions, Map<Class<?>, PropertyHandler> propertyHandlers) {
        if (decision == null) {
            throw new IllegalArgumentException("no decision provided");
        }

        if (decision.getExpression() == null || !(decision.getExpression() instanceof DecisionTable)) {
            throw new IllegalArgumentException("no decision table present in decision");
        }

        DecisionTable currentDecisionTable = (DecisionTable) decision.getExpression();

        // create execution context and audit trail
        MvelExecutionContext executionContext = MvelExecutionContextBuilder.build(decision, inputVariables,
            customExpressionFunctions, propertyHandlers);

        List<Map<String, Object>> decisionResult = null;
        RuleEngineExecutionResult executionResult;
        try {
            sanityCheckDecisionTable(currentDecisionTable);

            // evaluate decision table
            decisionResult = evaluateDecisionTable(currentDecisionTable, executionContext);
        } catch (FlowableException fe) {
            logger.error("decision table execution sanity check failed", fe);
            executionContext.getAuditContainer().setFailed();
            executionContext.getAuditContainer().setExceptionMessage(getExceptionMessage(fe));
        } finally {
            // end audit trail
            executionContext.getAuditContainer().stopAudit();

            // create result container
            executionResult = new RuleEngineExecutionResult(decisionResult, executionContext.getAuditContainer());
        }

        return executionResult;
    }

    protected List<Map<String, Object>> evaluateDecisionTable(DecisionTable decisionTable, MvelExecutionContext executionContext) {
        logger.debug("Start table evaluation: {}", decisionTable.getId());


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
                Boolean ruleResult = executeRule(rule, executionContext);

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
                    if (((ContinueEvaluatingBehavior) getHitPolicyBehavior(decisionTable.getHitPolicy())).shouldContinueEvaluating(ruleResult) == false) {
                        logger.debug("Stopping execution; hit policy {} specific behaviour", decisionTable.getHitPolicy());
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
                ((ComposeDecisionResultBehavior) getHitPolicyBehavior(decisionTable.getHitPolicy())).composeDecisionResults(executionContext);
            }

        } catch (FlowableException ade) {
            logger.error("decision table execution failed", ade);
            executionContext.getRuleResults().clear();
            executionContext.getAuditContainer().setFailed();
            executionContext.getAuditContainer().setExceptionMessage(getExceptionMessage(ade));
        }

        logger.debug("End table evaluation: {}", decisionTable.getId());

        return executionContext.getDecisionResults();
    }

    protected Boolean executeRule(DecisionRule rule, MvelExecutionContext executionContext) {
        if (rule == null) {
            throw new FlowableException("rule cannot be null");
        }

        logger.debug("Start rule {} evaluation", rule.getRuleNumber());

        // add audit entry
        executionContext.getAuditContainer().addRuleEntry(rule);

        Boolean conditionResult = Boolean.FALSE;

        // go through conditions
        for (RuleInputClauseContainer conditionContainer : rule.getInputEntries()) {

            // resetting value
            conditionResult = Boolean.FALSE;

            try {
                // if condition is empty condition result is TRUE
                if (StringUtils.isEmpty(conditionContainer.getInputEntry().getText())) {
                    conditionResult = Boolean.TRUE;
                } else {
                    conditionResult = executeInputExpressionEvaluation(conditionContainer, executionContext);
                }

                // add audit entry
                executionContext.getAuditContainer().addInputEntry(rule.getRuleNumber(), conditionContainer.getInputClause().getInputNumber(),
                    conditionContainer.getInputEntry().getId(), conditionResult);

                logger.debug("input entry {} ( {} {} ): {} ", conditionContainer.getInputEntry().getId(),
                    conditionContainer.getInputClause().getInputExpression().getText(),
                    conditionContainer.getInputEntry().getText(), conditionResult);
            } catch (FlowableDmnExpressionException adee) {
                // add failed audit entry
                executionContext.getAuditContainer().addInputEntry(rule.getRuleNumber(), conditionContainer.getInputClause().getInputNumber(),
                    conditionContainer.getInputEntry().getId(), getExceptionMessage(adee), conditionResult);
            } catch (FlowableException ade) {
                // add failed audit entry and rethrow
                executionContext.getAuditContainer().addInputEntry(rule.getRuleNumber(), conditionContainer.getInputClause().getInputNumber(),
                    conditionContainer.getInputEntry().getId(), getExceptionMessage(ade), null);
                throw ade;
            } catch (Exception e) {
                // add failed audit entry and rethrow
                executionContext.getAuditContainer().addInputEntry(rule.getRuleNumber(), conditionContainer.getInputClause().getInputNumber(),
                    conditionContainer.getInputEntry().getId(), getExceptionMessage(e), null);
                throw new FlowableException(getExceptionMessage(e), e);
            }

            // exit evaluation loop if a condition is evaluated false
            if (!conditionResult) {
                break;
            }

            // mark rule valid
            executionContext.getAuditContainer().markRuleValid(rule.getRuleNumber());
        }

        // mark rule end
        executionContext.getAuditContainer().markRuleEnd(rule.getRuleNumber());

        logger.debug("End rule {} evaluation", rule.getRuleNumber());
        return conditionResult;
    }

    protected Boolean executeInputExpressionEvaluation(RuleInputClauseContainer ruleContainer, MvelExecutionContext executionContext) {
        return MvelExpressionExecutor.executeInputExpression(ruleContainer.getInputClause(), ruleContainer.getInputEntry(), executionContext);
    }

    protected void executeOutputEntryAction(int ruleNumber, List<RuleOutputClauseContainer> ruleOutputContainers, HitPolicy hitPolicy, MvelExecutionContext executionContext) {
        logger.debug("Start conclusion processing");

        for (RuleOutputClauseContainer clauseContainer : ruleOutputContainers) {
            composeOutputEntryResult(ruleNumber, clauseContainer, hitPolicy, executionContext);
        }

        logger.debug("End conclusion processing");
    }

    protected void composeOutputEntryResult(int ruleNumber, RuleOutputClauseContainer ruleClauseContainer, HitPolicy hitPolicy, MvelExecutionContext executionContext) {
        logger.debug("Start evaluation conclusion {} of valid rule {}", ruleClauseContainer.getOutputClause().getOutputNumber(), ruleNumber);

        String outputVariableId = ruleClauseContainer.getOutputClause().getName();
        String outputVariableType = ruleClauseContainer.getOutputClause().getTypeRef();

        LiteralExpression outputEntryExpression = ruleClauseContainer.getOutputEntry();

        if (StringUtils.isNotEmpty(outputEntryExpression.getText())) {
            Object executionVariable = null;
            try {
                Object resultValue = MvelExpressionExecutor.executeOutputExpression(ruleClauseContainer.getOutputClause(), outputEntryExpression, executionContext);
                executionVariable = ExecutionVariableFactory.getExecutionVariable(outputVariableType, resultValue);

                // create result
                if (getHitPolicyBehavior(hitPolicy) instanceof ComposeRuleResultBehavior) {
                    ((ComposeRuleResultBehavior) getHitPolicyBehavior(hitPolicy)).composeRuleResult(ruleNumber, outputVariableId, executionVariable, executionContext);
                }

                // add audit entry
                executionContext.getAuditContainer().addOutputEntry(ruleNumber, ruleClauseContainer.getOutputClause().getOutputNumber(), outputEntryExpression.getId(), executionVariable);

                if (executionVariable != null) {
                    logger.debug("Created conclusion result: {} of type: {} with value {} ", outputVariableId, resultValue.getClass(), resultValue.toString());
                } else {
                    logger.warn("Could not create conclusion result");
                }
            } catch (FlowableException ade) {
                // clear result variables
                executionContext.getRuleResults().clear();

                // add failed audit entry and rethrow
                executionContext.getAuditContainer().addOutputEntry(ruleNumber, ruleClauseContainer.getOutputClause().getOutputNumber(), outputEntryExpression.getId(), getExceptionMessage(ade), executionVariable);
                throw ade;

            } catch (Exception e) {
                // clear result variables
                executionContext.getRuleResults().clear();

                // add failed audit entry and rethrow
                executionContext.getAuditContainer().addOutputEntry(ruleNumber, ruleClauseContainer.getOutputClause().getOutputNumber(), outputEntryExpression.getId(), getExceptionMessage(e), executionVariable);
                throw new FlowableException(getExceptionMessage(e), e);
            }
        } else {
            logger.debug("Expression is empty");

            // add empty audit entry
            executionContext.getAuditContainer().addOutputEntry(ruleNumber, ruleClauseContainer.getOutputClause().getOutputNumber(), outputEntryExpression.getId(), null);
        }

        logger.debug("End evaluation conclusion {} of valid rule {}", ruleClauseContainer.getOutputClause().getOutputNumber(), ruleNumber);
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

            logger.error(hitPolicyBehaviorNotFoundMessage);

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
}