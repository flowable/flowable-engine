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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.dmn.api.ExpressionExecution;
import org.flowable.dmn.api.RuleEngineExecutionResult;
import org.flowable.dmn.api.RuleExecutionAuditContainer;
import org.flowable.dmn.engine.FlowableDmnExpressionException;
import org.flowable.dmn.engine.RuleEngineExecutor;
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
     * @param decision
     *            the DMN decision
     * @param inputVariables
     *            map with input variables
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

        if (decisionTable == null || decisionTable.getRules().isEmpty()) {
            throw new IllegalArgumentException("no rules present in table");
        }

        if (executionContext == null) {
            throw new FlowableException("no execution context available");
        }

        logger.debug("Start table evaluation: {}", decisionTable.getId());

        try {
            // evaluate rule conditions
            Map<Integer, List<RuleOutputClauseContainer>> validRuleOutputEntries = new HashMap<>();

            // initialize rule counter
            // currently this is the only way to identify the rules
            int ruleCounter = 0;

            for (DecisionRule rule : decisionTable.getRules()) {

                Boolean ruleResult = executeRule(ruleCounter, rule, executionContext);

                if (ruleResult) {
                    validRuleOutputEntries.put(ruleCounter, rule.getOutputEntries());
                }

                if (!shouldContinueEvaluating(decisionTable.getHitPolicy(), ruleResult)) {
                    break;
                }

                ruleCounter++;
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

    protected Boolean executeRule(int ruleRowIndex, DecisionRule rule, MvelExecutionContext executionContext) {

        if (rule == null) {
            throw new FlowableException("rule cannot be null");
        }

        logger.debug("Start rule evaluation");

        // add audit entry
        executionContext.getAuditContainer().addRuleEntry();

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
                executionContext.getAuditContainer().addInputEntry(ruleRowIndex, conditionContainer.getInputEntry().getId(), conditionResult);

                logger.debug("input entry {} ( {} {} ): {} ", conditionContainer.getInputEntry().getId(),
                        conditionContainer.getInputClause().getInputExpression().getText(),
                        conditionContainer.getInputEntry().getText(), conditionResult);

            } catch (FlowableDmnExpressionException adee) {

                // add failed audit entry
                executionContext.getAuditContainer().addInputEntry(ruleRowIndex, conditionContainer.getInputEntry().getId(),
                        getExceptionMessage(adee), conditionResult);

            } catch (FlowableException ade) {

                // add failed audit entry and rethrow
                executionContext.getAuditContainer().addInputEntry(ruleRowIndex, conditionContainer.getInputEntry().getId(),
                        getExceptionMessage(ade), null);
                throw ade;

            } catch (Exception e) {

                // add failed audit entry and rethrow
                executionContext.getAuditContainer().addInputEntry(ruleRowIndex, conditionContainer.getInputEntry().getId(),
                        getExceptionMessage(e), null);
                throw new FlowableException(getExceptionMessage(e), e);
            }

            // exit evaluation loop if a condition is evaluated false
            if (!conditionResult) {
                break;
            }
        }

        // mark rule end
        executionContext.getAuditContainer().markRuleEnd(ruleRowIndex);

        logger.debug("End rule evaluation");
        return conditionResult;
    }

    protected Boolean executeInputExpressionEvaluation(RuleInputClauseContainer ruleContainer, MvelExecutionContext executionContext) {

        return MvelExpressionExecutor.executeInputExpression(ruleContainer.getInputClause(), ruleContainer.getInputEntry(), executionContext);
    }

    protected void executeOutputEntryAction(int ruleRowIndex, List<RuleOutputClauseContainer> ruleOutputContainers, HitPolicy hitPolicy, MvelExecutionContext executionContext) {

        logger.debug("Start conclusion processing");

        int ruleConclusionIndex = 0;
        for (RuleOutputClauseContainer clauseContainer : ruleOutputContainers) {
            composeOutputEntryResult(ruleRowIndex, ruleConclusionIndex, clauseContainer, hitPolicy, executionContext);
            ruleConclusionIndex++;
        }

        logger.debug("End conclusion processing");
    }

    protected void composeOutputEntryResult(int ruleRowIndex, int ruleConclusionIndex, RuleOutputClauseContainer ruleClauseContainer, HitPolicy hitPolicy, MvelExecutionContext executionContext) {

        String outputVariableId = ruleClauseContainer.getOutputClause().getName();
        String outputVariableType = ruleClauseContainer.getOutputClause().getTypeRef();

        LiteralExpression outputEntryExpression = ruleClauseContainer.getOutputEntry();

        if (StringUtils.isNotEmpty(outputEntryExpression.getText())) {
            Object executionVariable = null;
            try {
                Object resultValue = MvelExpressionExecutor.executeOutputExpression(ruleClauseContainer.getOutputClause(), outputEntryExpression, executionContext);
                executionVariable = ExecutionVariableFactory.getExecutionVariable(outputVariableType, resultValue);

                // check validity
                evaluateRuleConclusion(resultValue, ruleRowIndex, ruleConclusionIndex, hitPolicy, executionContext);

                // add result variable
                executionContext.getResultVariables().put(outputVariableId, executionVariable);

                // add audit entry
                executionContext.getAuditContainer().addOutputEntry(ruleRowIndex, outputEntryExpression.getId(), executionVariable);

                if (executionVariable != null) {
                    logger.debug("Created conclusion result: {} of type: {} with value {} ", outputVariableId, resultValue.getClass(), resultValue.toString());
                } else {
                    logger.warn("Could not create conclusion result");
                }
            } catch (FlowableException ade) {

                // add failed audit entry and rethrow
                executionContext.getAuditContainer().addOutputEntry(ruleRowIndex, outputEntryExpression.getId(), getExceptionMessage(ade), executionVariable);
                throw ade;

            } catch (Exception e) {

                // add failed audit entry and rethrow
                executionContext.getAuditContainer().addOutputEntry(ruleRowIndex, outputEntryExpression.getId(), getExceptionMessage(e), executionVariable);
                throw new FlowableException(getExceptionMessage(e), e);
            }
        } else {
            // add empty audit entry
            executionContext.getAuditContainer().addOutputEntry(ruleRowIndex, outputEntryExpression.getId(), null);
        }
    }

    protected void evaluateRuleConclusion(Object resultValue, int ruleRowIndex, int ruleConclusionIndex, HitPolicy hitPolicy, MvelExecutionContext executionContext) {
        if (hitPolicy == HitPolicy.ANY) {
            checkHitPolicyAnyValidity(resultValue, ruleRowIndex, ruleConclusionIndex, executionContext);
        }
    }

    protected void checkHitPolicyAnyValidity(Object resultValue, int ruleRowIndex, int ruleConclusionIndex, MvelExecutionContext executionContext) {
        int validityRuleRowIndex = 0;

        for (RuleExecutionAuditContainer ruleExecutionAuditContainer : executionContext.getAuditContainer().getRuleExecutions()) {

            if (!ruleExecutionAuditContainer.getConclusionResults().isEmpty() &&
                ruleExecutionAuditContainer.getConclusionResults().size() > ruleConclusionIndex) {

                ExpressionExecution expressionExecution = ruleExecutionAuditContainer.getConclusionResults().get(ruleConclusionIndex);

                // conclusion value cannot be the same as for other valid rules
                if (expressionExecution != null && expressionExecution.getResult() != null && !expressionExecution.getResult().equals(resultValue)) {
                    logger.warn("HitPolicy ANY violated: conclusion {} of rule {} with value {} is the same as for rule {}", ruleConclusionIndex, ruleRowIndex, resultValue, validityRuleRowIndex);
                    throw new FlowableException("HitPolicy ANY violated; conclusion value is not the same");
                }
            }
            validityRuleRowIndex++;
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
