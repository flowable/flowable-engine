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
package org.flowable.cmmn.engine.impl.agenda.operation;

import static org.flowable.cmmn.engine.impl.variable.CmmnAggregation.COUNTER_VAR_PREFIX;
import static org.flowable.cmmn.engine.impl.variable.CmmnAggregation.COUNTER_VAR_VALUE_SEPARATOR;
import static org.flowable.cmmn.engine.impl.variable.CmmnAggregation.aggregateComplete;
import static org.flowable.cmmn.engine.impl.variable.CmmnAggregation.createScopedVariableAggregationVariableInstance;
import static org.flowable.cmmn.engine.impl.variable.CmmnAggregation.groupAggregationsByTarget;
import static org.flowable.cmmn.engine.impl.variable.CmmnAggregation.groupVariableInstancesByName;
import static org.flowable.cmmn.engine.impl.variable.CmmnAggregation.resolveVariableAggregator;
import static org.flowable.cmmn.engine.impl.variable.CmmnAggregation.sortVariablesByCounter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.delegate.PlanItemVariableAggregator;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.delegate.BaseVariableAggregatorContext;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.runtime.StateTransition;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.ExpressionUtil;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.cmmn.model.VariableAggregationDefinition;
import org.flowable.cmmn.model.VariableAggregationDefinitions;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * Operation that moves a given {@link org.flowable.cmmn.api.runtime.PlanItemInstance} to a terminal state (completed, terminated or failed).
 *
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public abstract class AbstractMovePlanItemInstanceToTerminalStateOperation extends AbstractChangePlanItemInstanceStateOperation {

    public AbstractMovePlanItemInstanceToTerminalStateOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }

    @Override
    public void run() {

        // Need to capture the original state before the super.run() will change it
        String originalState = planItemInstanceEntity.getState();

        // Not overriding the internalExecute, as that's meant for subclasses of this operation.
        super.run();

        VariableAggregationDefinitions aggregations = getVariableAggregations();
        // There is a fake plan item instance created for waiting for repetition
        // This instance does not follow the same lifecycle and thus we should not aggregate variables for it
        boolean shouldAggregate = aggregations != null && PlanItemInstanceState.ACTIVE_STATES.contains(originalState);
        if (shouldAggregate && shouldAggregateForSingleInstance()) {
            aggregateVariablesForSingleInstance(planItemInstanceEntity, aggregations);
        }

        if (!isNoop) {  // The super.run() could have marked this as a no-op. No point in continuing.

            String plannedNewState = getNewState();

            if (isRepeatingOnDelete(originalState, plannedNewState) && !isWaitingForRepetitionPlanItemInstanceExists(planItemInstanceEntity)) {

                // Create new repeating instance
                PlanItemInstanceEntity newPlanItemInstanceEntity = copyAndInsertPlanItemInstance(commandContext, planItemInstanceEntity, true, false);

                if (planItemInstanceEntity.getPlanItem() != null && planItemInstanceEntity.getPlanItem().getPlanItemDefinition() instanceof EventListener) {
                    CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceOperation(newPlanItemInstanceEntity);

                } else {

                    String oldState = newPlanItemInstanceEntity.getState();
                    String newState = PlanItemInstanceState.WAITING_FOR_REPETITION;
                    newPlanItemInstanceEntity.setState(newState);
                    CommandContextUtil.getCmmnEngineConfiguration(commandContext).getListenerNotificationHelper()
                        .executeLifecycleListeners(commandContext, newPlanItemInstanceEntity, oldState, newState);

                    // Plan item creation "for Repetition"
                    CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceForRepetitionOperation(newPlanItemInstanceEntity);

                    // Plan item doesn't have entry criteria (checked in the if condition) and immediately goes to ACTIVE
                    if (hasRepetitionRuleAndNoEntryCriteria(planItemInstanceEntity.getPlanItem())) {
                        CommandContextUtil.getAgenda(commandContext).planActivatePlanItemInstanceOperation(newPlanItemInstanceEntity, null);
                    }
                }

            } else if (shouldAggregate) {
                aggregateVariablesForAllInstances(planItemInstanceEntity, aggregations);

            }

            removeSentryRelatedData();
        }
    }

    protected VariableAggregationDefinitions getVariableAggregations() {
        RepetitionRule repetitionRule = ExpressionUtil.getRepetitionRule(planItemInstanceEntity);
        return repetitionRule != null ? repetitionRule.getAggregations() : null;
    }

    /**
     * Implementing classes should be aware that unlike extending from AbstractChangePlanItemInstanceStateOperation, this
     * method will be executed just before the deleting the entity
     */
    @Override
    protected abstract void internalExecute();

    public boolean isRepeatingOnDelete(String originalState, String newState) {
        
        // If there are no entry criteria and the repetition rule evaluates to true: a new instance needs to be created.

        CaseInstanceEntity caseInstance = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(planItemInstanceEntity.getCaseInstanceId());
        if (CaseInstanceState.isInTerminalState(caseInstance)) {
            return false;
        }
        
        PlanItem planItem = planItemInstanceEntity.getPlanItem();
        if (isEvaluateRepetitionRule() && hasRepetitionRuleAndNoEntryCriteria(planItem)) {
            return ExpressionUtil.evaluateRepetitionRule(commandContext, planItemInstanceEntity, planItemInstanceEntity.getStagePlanItemInstanceEntity());
        }

        // If the plan item instance is in AVAILABLE (and the new state is terminated), and it's repeatable and it gets terminated
        // this means it has never moved away from available.
        // This means there never was a wait_for_repetition instance created (because the plan item instance
        // never goes back to available but to wait_for_repetition).
        // In this specific case, we need to create the wait_for_repetition for future repetitions
        if (PlanItemInstanceState.AVAILABLE.equals(originalState)
                && PlanItemInstanceState.TERMINATED.equals(newState)
                && hasRepetitionRuleEntryCriteria(planItem)
                && !hasRepetitionOnCollection(planItem)
                && isWithoutStageOrParentIsNotTerminated(planItemInstanceEntity)) { // only when the parent is not yet terminated, a new instance should be created
            return true; // the repetition rule doesn't matter, as it can happen on any entry condition that becomes true
        }

        return false;
    }

    public boolean isWithoutStageOrParentIsNotTerminated(PlanItemInstanceEntity planItemInstanceEntity) {
        return planItemInstanceEntity.getStagePlanItemInstanceEntity() == null
                || !PlanItemInstanceState.isInTerminalState(planItemInstanceEntity.getStagePlanItemInstanceEntity());
    }

    /**
     * Returns true, if the given plan item has a repetition rule, but no entry criteria to be satisfied and no collection based repetition. A collection
     * based repetition is similar to entry criteria as it needs to be available in order to repeat the plan item.
     *
     * @param planItem the plan item to test
     * @return true, if the plan item has a repetition rule without any conditions like entry criteria or a collection to be based on for repetition
     */
    public boolean hasRepetitionRuleAndNoEntryCriteria(PlanItem planItem) {
        return planItem != null
            && planItem.getEntryCriteria().isEmpty()
            && planItem.getItemControl() != null
            && planItem.getItemControl().getRepetitionRule() != null
            && !planItem.getItemControl().getRepetitionRule().hasCollectionVariable();
    }

    public boolean hasRepetitionOnCollection(PlanItem planItem) {
        return planItem != null
            && planItem.getItemControl() != null
            && planItem.getItemControl().getRepetitionRule() != null
            && planItem.getItemControl().getRepetitionRule().hasCollectionVariable();
    }

    public boolean hasRepetitionRuleEntryCriteria(PlanItem planItem) {
        return planItem != null
            && !planItem.getEntryCriteria().isEmpty()
            && planItem.getItemControl() != null
            && planItem.getItemControl().getRepetitionRule() != null;
    }

    public boolean isWaitingForRepetitionPlanItemInstanceExists(PlanItemInstanceEntity planItemInstanceEntity) {
        PlanItemInstanceEntity stagePlanItemInstanceEntity = planItemInstanceEntity.getStagePlanItemInstanceEntity();
        if (stagePlanItemInstanceEntity != null) {
            List<PlanItemInstanceEntity> childPlanItemInstances = stagePlanItemInstanceEntity.getChildPlanItemInstances();
            if (childPlanItemInstances != null && !childPlanItemInstances.isEmpty()) {
                return childPlanItemInstances.stream()
                    .anyMatch(p -> Objects.equals(p.getPlanItem().getId(), planItemInstanceEntity.getPlanItem().getId())
                        && PlanItemInstanceState.WAITING_FOR_REPETITION.equals(p.getState()));
            }
        }
        return false;
    }

    protected void completeChildPlanItemInstances() {
        completeChildPlanItemInstances(null);
    }

    protected void completeChildPlanItemInstances(String exitCriterionId) {
        for (PlanItemInstanceEntity child : planItemInstanceEntity.getChildPlanItemInstances()) {
            if (StateTransition.isPossible(child, PlanItemTransition.COMPLETE)) {
                CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(child);
            }
        }
    }
    
    protected void exitChildPlanItemInstances() {
        exitChildPlanItemInstances(null);
    }

    protected void exitChildPlanItemInstances(String exitCriterionId) {
        for (PlanItemInstanceEntity child : planItemInstanceEntity.getChildPlanItemInstances()) {
            if (StateTransition.isPossible(child, PlanItemTransition.EXIT)) {
                // don't propagate the exit event type and exit type to child plan items, it only has an impact where it was set using the exit sentry
                CommandContextUtil.getAgenda(commandContext).planExitPlanItemInstanceOperation(child, exitCriterionId, null, null);
            }
        }
    }

    protected void aggregateVariablesForSingleInstance(PlanItemInstanceEntity planItemInstanceEntity, VariableAggregationDefinitions aggregations) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);

        // Gathered variables are stored on the finished plan item instances
        VariableServiceConfiguration variableServiceConfiguration = cmmnEngineConfiguration.getVariableServiceConfiguration();
        VariableService variableService = variableServiceConfiguration.getVariableService();

        for (VariableAggregationDefinition aggregation : aggregations.getAggregations()) {

            VariableInstanceEntity aggregatedVarInstance = aggregateComplete(planItemInstanceEntity, aggregation, cmmnEngineConfiguration);
            if (aggregatedVarInstance != null) {
                variableService.insertVariableInstance(aggregatedVarInstance);

                String targetVarName = aggregatedVarInstance.getName();

                int repetitionCounter = getRepetitionCounter(planItemInstanceEntity);
                String repetitionValue = aggregatedVarInstance.getId() + COUNTER_VAR_VALUE_SEPARATOR + repetitionCounter;
                VariableInstanceEntity counterVarInstance = createScopedVariableAggregationVariableInstance(COUNTER_VAR_PREFIX + targetVarName,
                        aggregatedVarInstance.getScopeId(), aggregatedVarInstance.getSubScopeId(), repetitionValue, variableServiceConfiguration);
                variableService.insertVariableInstance(counterVarInstance);
            }
        }

    }

    protected void aggregateVariablesForAllInstances(PlanItemInstanceEntity planItemInstanceEntity, VariableAggregationDefinitions aggregations) {

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);

        List<PlanItemInstanceEntity> planItemInstances;

        if (StringUtils.isNotEmpty(planItemInstanceEntity.getStageInstanceId())) {
            planItemInstances = cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                    .findByStageInstanceIdAndPlanItemId(planItemInstanceEntity.getStageInstanceId(), planItemInstanceEntity.getPlanItem().getId());
        } else {
            planItemInstances = cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                    .findByCaseInstanceIdAndPlanItemId(planItemInstanceEntity.getCaseInstanceId(), planItemInstanceEntity.getPlanItem().getId());
        }

        if (planItemInstances == null || planItemInstances.isEmpty()) {
            return;
        }

        // All instances should be in the terminal state to apply the variable gathering
        for (PlanItemInstanceEntity planItemInstance : planItemInstances) {
            if (!PlanItemInstanceState.TERMINAL_STATES.contains(planItemInstance.getState())) {
                return;
            }
        }

        String subScopeId = planItemInstanceEntity.getStageInstanceId();
        if (subScopeId == null) {
            subScopeId = planItemInstanceEntity.getCaseInstanceId();
        }

        // Gathered variables are stored on finished the plan item instances
        VariableService variableService = cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableService();
        List<VariableInstanceEntity> variableInstances = variableService.createInternalVariableInstanceQuery()
                .subScopeId(subScopeId)
                .scopeType(ScopeTypes.CMMN_VARIABLE_AGGREGATION)
                .list();

        Map<String, VariableAggregationDefinition> aggregationsByTarget = groupAggregationsByTarget(planItemInstanceEntity, aggregations.getAggregations(), cmmnEngineConfiguration);

        Map<String, List<VariableInstance>> instancesByName = groupVariableInstancesByName(variableInstances);

        boolean aggregateMulti = shouldAggregateForMultipleInstances();

        for (Map.Entry<String, VariableAggregationDefinition> entry : aggregationsByTarget.entrySet()) {
            String varName = entry.getKey();
            if (aggregateMulti) {

                VariableAggregationDefinition aggregation = aggregationsByTarget.get(varName);
                PlanItemVariableAggregator aggregator = resolveVariableAggregator(aggregation, planItemInstanceEntity);

                List<VariableInstance> counterVariables = instancesByName.getOrDefault(COUNTER_VAR_PREFIX + varName, Collections.emptyList());
                List<VariableInstance> varValues = instancesByName.getOrDefault(varName, Collections.emptyList());

                sortVariablesByCounter(varValues, counterVariables);

                Object value = aggregator.aggregateMultiVariables(planItemInstanceEntity, varValues, BaseVariableAggregatorContext.complete(aggregation));

                if (aggregation.isStoreAsTransientVariable()) {
                    planItemInstanceEntity.getParentVariableScope().setTransientVariable(varName, value);
                } else {
                    planItemInstanceEntity.getParentVariableScope().setVariable(varName, value);
                }
            } else {
                planItemInstanceEntity.getParentVariableScope().removeVariable(varName);
            }

        }

        variableInstances.forEach(variableService::deleteVariableInstance);
    }

    /**
     * Whether variable aggregation should be done when a single instance completes.
     * This does not need to check whether the plan item instance has variable aggregations,
     * that is the same for all instances.
     * e.g. When an instance completes normally we should aggregate the data, but if it terminates we shouldn't
     */
    protected abstract boolean shouldAggregateForSingleInstance();

    /**
     * Whether multi aggregation needs to be done.
     * This does not need to check whether the plan item instance has variable aggregations,
     * that is the same for all instances.
     * e.g. Multi aggregation needs to be done when we do a normal completion, but not when the plan items are terminated
     */
    protected abstract boolean shouldAggregateForMultipleInstances();

    public abstract boolean isEvaluateRepetitionRule();
    
}
