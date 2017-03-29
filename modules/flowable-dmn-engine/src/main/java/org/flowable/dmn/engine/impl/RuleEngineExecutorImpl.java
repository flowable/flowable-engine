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
import org.flowable.dmn.api.ExpressionExecution;
import org.flowable.dmn.api.RuleEngineExecutionResult;
import org.flowable.dmn.api.RuleExecutionAuditContainer;
import org.flowable.dmn.engine.FlowableDmnExpressionException;
import org.flowable.dmn.engine.RuleEngineExecutor;
import org.flowable.dmn.engine.impl.context.Context;
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

        // evaluate decision table
        Map<String, Object> resultVariables = evaluateDecisionTable(currentDecisionTable, executionContext);

        // end audit trail
        executionContext.getAuditContainer().stopAudit(resultVariables);

        // create result container
        RuleEngineExecutionResult executionResult = new RuleEngineExecutionResult(resultVariables, executionContext.getAuditContainer());

        return executionResult;
    }

    protected Map<String, Object> evaluateDecisionTable(DecisionTable decisionTable, MvelExecutionContext executionContext) {
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
                    evaluateRuleValidity(ruleResult, rule.getRuleNumber(), decisionTable.getHitPolicy(), executionContext);

                    // add valid rule output(s)
                    validRuleOutputEntries.put(rule.getRuleNumber(), rule.getOutputEntries());
                }

                if (!shouldContinueEvaluating(decisionTable.getHitPolicy(), ruleResult)) {
                    break;
                }
            }

            // evaluate rule conclusions
            for (Map.Entry<Integer, List<RuleOutputClauseContainer>> entry : validRuleOutputEntries.entrySet()) {
                executeOutputEntryAction(entry.getKey(), entry.getValue(), decisionTable.getHitPolicy(), executionContext);
            }

        } catch (FlowableException ade) {
            logger.error("decision table execution failed", ade);
            executionContext.getAuditContainer().setFailed();
            executionContext.getAuditContainer().setExceptionMessage(getExceptionMessage(ade));
        }

        logger.debug("End table evaluation: {}", decisionTable.getId());

        return executionContext.getResultVariables();
    }

    protected Boolean shouldContinueEvaluating(HitPolicy hitPolicy, Boolean ruleResult) {
        Boolean shouldContinue = Boolean.TRUE;

        if (hitPolicy == HitPolicy.FIRST && ruleResult) {
            logger.debug("Stopping execution: rule is valid and Hit Policy is FIRST");
            shouldContinue = Boolean.FALSE;
        }

        return shouldContinue;
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

                // check validity
                evaluateRuleConclusionValidity(executionVariable, ruleNumber, ruleClauseContainer.getOutputClause().getOutputNumber(), hitPolicy, executionContext);

                // create result
                composeResult(hitPolicy, outputVariableId, executionVariable, executionContext);

                // add audit entry
                executionContext.getAuditContainer().addOutputEntry(ruleNumber, ruleClauseContainer.getOutputClause().getOutputNumber(), outputEntryExpression.getId(), executionVariable);

                if (executionVariable != null) {
                    logger.debug("Created conclusion result: {} of type: {} with value {} ", outputVariableId, resultValue.getClass(), resultValue.toString());
                } else {
                    logger.warn("Could not create conclusion result");
                }
            } catch (FlowableException ade) {
                // clear result variables
                executionContext.getResultVariables().clear();

                // add failed audit entry and rethrow
                executionContext.getAuditContainer().addOutputEntry(ruleNumber, ruleClauseContainer.getOutputClause().getOutputNumber(), outputEntryExpression.getId(), getExceptionMessage(ade), executionVariable);
                throw ade;

            } catch (Exception e) {
                // clear result variables
                executionContext.getResultVariables().clear();

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

    protected void evaluateRuleValidity(Boolean ruleResult, int ruleNumber, HitPolicy hitPolicy, MvelExecutionContext executionContext) {
        if (hitPolicy == HitPolicy.UNIQUE) {
            checkHitPolicyUniqueValidity(ruleResult, ruleNumber, executionContext);
        }
    }

    protected void evaluateRuleConclusionValidity(Object resultValue, int ruleNumber, int ruleConclusionIndex, HitPolicy hitPolicy, MvelExecutionContext executionContext) {
        if (hitPolicy == HitPolicy.ANY) {
            checkHitPolicyAnyValidity(resultValue, ruleNumber, ruleConclusionIndex, executionContext);
        }
    }

    protected void checkHitPolicyAnyValidity(Object resultValue, int ruleNumber, int ruleConclusionNumber, MvelExecutionContext executionContext) {
        for (Map.Entry<Integer, RuleExecutionAuditContainer> entry : executionContext.getAuditContainer().getRuleExecutions().entrySet()) {
            if (entry.getKey().equals(ruleNumber) == false &&
                !entry.getValue().getConclusionResults().isEmpty() &&
                entry.getValue().getConclusionResults().size() >= ruleConclusionNumber) {

                ExpressionExecution expressionExecution = entry.getValue().getConclusionResults().get(ruleConclusionNumber);

                // conclusion value must be the same as for other valid rules
                if (expressionExecution != null && expressionExecution.getResult() != null && !expressionExecution.getResult().equals(resultValue)) {
                    String hitPolicyViolatedMessage = String.format("HitPolicy ANY violated: conclusion %d of rule %d is not the same as for rule %d", ruleConclusionNumber, ruleNumber, entry.getKey());

                    logger.warn(hitPolicyViolatedMessage);

                    if (Context.getDmnEngineConfiguration().isStrictMode()) {
                        throw new FlowableException("HitPolicy ANY violated");
                    }
                }
            }
        }
    }

    protected void checkHitPolicyUniqueValidity(Boolean ruleResult, int ruleNumber, MvelExecutionContext executionContext) {
        if (ruleResult) {
            for (Map.Entry<Integer, RuleExecutionAuditContainer> entry : executionContext.getAuditContainer().getRuleExecutions().entrySet()) {
                if (entry.getKey().equals(ruleNumber) == false && entry.getValue().isValid()) {
                    String hitPolicyViolatedMessage = String.format("HitPolicy UNIQUE violated: rule %d is valid but rule %d was already valid", ruleNumber, entry.getKey());

                    logger.warn(hitPolicyViolatedMessage);

                    if (Context.getDmnEngineConfiguration().isStrictMode()) {
                        executionContext.getAuditContainer().getRuleExecutions().get(ruleNumber).setExceptionMessage(hitPolicyViolatedMessage);

                        throw new FlowableException("HitPolicy UNIQUE violated");
                    }
                }
            }
        }
    }

    protected void composeResult(HitPolicy hitPolicy, String outputVariableId, Object executionVariable, MvelExecutionContext executionContext) {
        if (hitPolicy == HitPolicy.RULE_ORDER) {
            Object resultVariable = executionContext.getResultVariables().get(outputVariableId);
            if (resultVariable == null) {
                resultVariable = new ArrayList<>();
            }
            if (resultVariable instanceof List) {
                ((List) resultVariable).add(executionVariable);

                // add result variable
                executionContext.getResultVariables().put(outputVariableId, resultVariable);
            } else {
                throw new FlowableException("HitPolicy RULE ORDER has wrong output variable type");
            }
        } else {
            // add result variable
            executionContext.getResultVariables().put(outputVariableId, executionVariable);
        }
    }


    protected String getExceptionMessage(Exception exception) {
        String exceptionMessage = null;
        if (exception.getCause() != null && exception.getCause().getMessage() != null) {
            exceptionMessage = exception.getCause().getMessage();
        } else {
            exceptionMessage = exception.getMessage();
        }
        return exceptionMessage;
    }
}
