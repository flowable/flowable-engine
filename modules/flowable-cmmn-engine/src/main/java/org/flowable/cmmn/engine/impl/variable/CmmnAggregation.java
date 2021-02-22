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
package org.flowable.cmmn.engine.impl.variable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemVariableAggregator;
import org.flowable.cmmn.api.delegate.PlanItemVariableAggregatorContext;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.delegate.BaseVariableAggregatorContext;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.DelegateExpressionUtil;
import org.flowable.cmmn.engine.impl.util.ExpressionUtil;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.cmmn.model.VariableAggregationDefinition;
import org.flowable.cmmn.model.VariableAggregationDefinitions;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.variable.api.delegate.VariableScope;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Filip Hrisafov
 */
public class CmmnAggregation {

    public static final String COUNTER_VAR_PREFIX = "__flowableCounter__";
    public static final String COUNTER_VAR_VALUE_SEPARATOR = "###";

    protected final String planItemInstanceId;

    public CmmnAggregation(String planItemInstanceId) {
        this.planItemInstanceId = planItemInstanceId;
    }

    public String getPlanItemInstanceId() {
        return planItemInstanceId;
    }

    /**
     * Create an aggregated variable instance for a completed plan item.
     *
     * @param planItemInstance the planItemInstance which completed
     * @param aggregation the aggregation definition
     * @param cmmnEngineConfiguration the cmmn engine configuration
     * @return the created variables (not yet saved), or {@code null} if no name could be resolved for the variable
     */
    public static VariableInstanceEntity aggregateComplete(DelegatePlanItemInstance planItemInstance,
            VariableAggregationDefinition aggregation, CmmnEngineConfiguration cmmnEngineConfiguration) {
        return aggregate(planItemInstance, BaseVariableAggregatorContext.complete(aggregation), cmmnEngineConfiguration);
    }

    /**
     * Create an aggregated variable instance for the given aggregation context.
     *
     * @param planItemInstance the planItemInstance
     * @param aggregationContext the aggregation context
     * @param cmmnEngineConfiguration the cmmn engine configuration
     * @return the created variables (not yet saved), or {@code null} if no name could be resolved for the variable
     */
    public static VariableInstanceEntity aggregate(DelegatePlanItemInstance planItemInstance,
            PlanItemVariableAggregatorContext aggregationContext, CmmnEngineConfiguration cmmnEngineConfiguration) {
        VariableAggregationDefinition aggregation = aggregationContext.getDefinition();
        PlanItemVariableAggregator aggregator = resolveVariableAggregator(aggregation, planItemInstance);
        String targetVarName = getAggregationTargetVarName(aggregation, planItemInstance, cmmnEngineConfiguration);

        if (targetVarName != null) {
            return aggregate(planItemInstance, aggregationContext, cmmnEngineConfiguration, aggregator, targetVarName);
        }

        return null;
    }

    /**
     * @param planItemInstance the planItemInstance
     * @param aggregationContext the aggregation context
     * @param cmmnEngineConfiguration the cmmn engine configuration
     * @param aggregator the aggregator that should be used to perform the aggregation
     * @param targetVarName the name of the variable, never {@code null}
     * @return the created variables (not yet saved), or {@code null} if no name could be resolved for the variable
     */
    public static VariableInstanceEntity aggregate(DelegatePlanItemInstance planItemInstance,
            PlanItemVariableAggregatorContext aggregationContext, CmmnEngineConfiguration cmmnEngineConfiguration, PlanItemVariableAggregator aggregator,
            String targetVarName) {

        VariableServiceConfiguration variableServiceConfiguration = cmmnEngineConfiguration.getVariableServiceConfiguration();

        Object aggregatedValue = aggregator.aggregateSingleVariable(planItemInstance, aggregationContext);

        String caseInstanceId = planItemInstance.getCaseInstanceId();
        String subScopeId = planItemInstance.getStageInstanceId();
        if (subScopeId == null) {
            subScopeId = caseInstanceId;
        }

        return createScopedVariableAggregationVariableInstance(targetVarName, caseInstanceId, subScopeId, aggregatedValue, variableServiceConfiguration);
    }

    /**
     * Aggregate an overview value for the scope and variable name
     *
     * @param parentScopeId the id of the scope for which the overview needs to be done
     * @param targetVarName the name of the variable
     * @return the aggregated variable value
     */
    public static Object aggregateOverview(String parentScopeId, String targetVarName, CommandContext commandContext) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        PlanItemInstanceEntity parent = cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                .findById(parentScopeId);

        RepetitionRule repetitionRule = ExpressionUtil.getRepetitionRule(parent);
        if (repetitionRule != null) {
            return aggregateOverviewForRepetition(parent, repetitionRule, targetVarName, cmmnEngineConfiguration);
        }

        // TODO aggregation overview for non multi instance
        return null;

    }

    /**
     * Create an overview value for a repetition plan item
     *
     * @param planItemInstance one of the repetition plan item instances
     * @param targetVarName the name of the variable
     * @param cmmnEngineConfiguration the cmmn engine configuration
     * @return the overview value for repetition plan items
     */
    public static Object aggregateOverviewForRepetition(PlanItemInstanceEntity planItemInstance, RepetitionRule repetitionRule, String targetVarName,
            CmmnEngineConfiguration cmmnEngineConfiguration) {
        VariableAggregationDefinitions aggregations = repetitionRule.getAggregations();
        if (aggregations == null) {
            // This should never happen as the planItemInstance is a repetition and we are doing an aggregation, but we are being extra safe
            return null;
        }

        String subScopeId = planItemInstance.getStageInstanceId();
        List<PlanItemInstanceEntity> repetitionPlanItems;
        if (subScopeId == null) {
            subScopeId = planItemInstance.getCaseInstanceId();
            repetitionPlanItems = cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                    .findByCaseInstanceIdAndPlanItemId(subScopeId, planItemInstance.getPlanItem().getId());
        } else {
            repetitionPlanItems = cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                    .findByStageInstanceIdAndPlanItemId(subScopeId, planItemInstance.getPlanItem().getId());
        }

        VariableServiceConfiguration variableServiceConfiguration = cmmnEngineConfiguration.getVariableServiceConfiguration();
        VariableService variableService = variableServiceConfiguration.getVariableService();
        // Find all variable instances for already plan item instances with the same sub scope
        List<VariableInstanceEntity> instances = variableService.createInternalVariableInstanceQuery()
                .subScopeId(subScopeId)
                .scopeType(ScopeTypes.CMMN_VARIABLE_AGGREGATION)
                .names(Arrays.asList(targetVarName, COUNTER_VAR_PREFIX + targetVarName))
                .list();

        String elementIndexVariable = repetitionRule.getRepetitionCounterVariableName();

        VariableScope parentVariableScope = planItemInstance.getParentVariableScope();

        Map<String, VariableAggregationDefinition> aggregationsByTarget = groupAggregationsByTarget(parentVariableScope, aggregations.getOverviewAggregations(), cmmnEngineConfiguration);
        VariableAggregationDefinition aggregation = aggregationsByTarget.get(targetVarName);
        PlanItemVariableAggregator aggregator = resolveVariableAggregator(aggregation, planItemInstance);

        PlanItemVariableAggregatorContext aggregationContext = BaseVariableAggregatorContext.overview(aggregation);

        for (PlanItemInstanceEntity repetitionPlanItem : repetitionPlanItems) {
            // We need to create overview values for every single active child plan item
            if (PlanItemInstanceState.ACTIVE_STATES.contains(repetitionPlanItem.getState())) {

                VariableInstanceEntity aggregatedVarInstance = aggregate(repetitionPlanItem,
                        aggregationContext, cmmnEngineConfiguration, aggregator, targetVarName);
                aggregatedVarInstance.setId(repetitionPlanItem.getId() + targetVarName);
                instances.add(aggregatedVarInstance);

                Integer elementIndexValue = repetitionPlanItem.getVariable(elementIndexVariable, Integer.class);
                if (elementIndexValue == null) {
                    elementIndexValue = 0;
                }
                String counterValue = aggregatedVarInstance.getId() + COUNTER_VAR_VALUE_SEPARATOR + elementIndexValue;
                VariableInstanceEntity counterVarInstance = createScopedVariableAggregationVariableInstance(COUNTER_VAR_PREFIX + targetVarName,
                        aggregatedVarInstance.getScopeId(), aggregatedVarInstance.getSubScopeId(), counterValue, variableServiceConfiguration);
                instances.add(counterVarInstance);
            }
        }

        if (!instances.isEmpty()) {
            Map<String, List<VariableInstance>> instancesByName = groupVariableInstancesByName(instances);

            List<VariableInstance> targetVariableInstances = instancesByName.get(targetVarName);
            List<VariableInstance> counterVariables = instancesByName.getOrDefault(COUNTER_VAR_PREFIX + targetVarName, Collections
                    .emptyList());

            sortVariablesByCounter(targetVariableInstances, counterVariables);

            return aggregator.aggregateMultiVariables(planItemInstance, targetVariableInstances, aggregationContext);
        }

        return null;
    }

    public static VariableInstanceEntity createScopedVariableAggregationVariableInstance(String varName, String scopeId, String subScopeId, Object value,
            VariableServiceConfiguration variableServiceConfiguration) {

        VariableService variableService = variableServiceConfiguration.getVariableService();

        VariableType variableType = variableServiceConfiguration.getVariableTypes().findVariableType(value);
        VariableInstanceEntity variableInstance = variableService.createVariableInstance(varName, variableType, value);
        variableInstance.setScopeId(scopeId);
        variableInstance.setSubScopeId(subScopeId);
        variableInstance.setScopeType(ScopeTypes.CMMN_VARIABLE_AGGREGATION);

        return variableInstance;
    }

    public static Map<String, List<VariableInstance>> groupVariableInstancesByName(List<? extends VariableInstance> instances) {
        return instances.stream().collect(Collectors.groupingBy(VariableInstance::getName));
    }

    public static PlanItemVariableAggregator resolveVariableAggregator(VariableAggregationDefinition aggregation, DelegatePlanItemInstance planItemInstance) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(aggregation.getImplementationType())) {
            return cmmnEngineConfiguration.getClassDelegateFactory().create(aggregation.getImplementation(), null);
        } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(aggregation.getImplementationType())) {
            Object delegate = DelegateExpressionUtil.resolveDelegateExpression(
                    cmmnEngineConfiguration.getExpressionManager().createExpression(aggregation.getImplementation()),
                    planItemInstance, null);

            if (delegate instanceof PlanItemVariableAggregator) {
                return (PlanItemVariableAggregator) delegate;
            }

            throw new FlowableIllegalArgumentException("Delegate expression " + aggregation.getImplementation() + " did not resolve to an implementation of " + PlanItemVariableAggregator.class);
        } else {
            return cmmnEngineConfiguration.getVariableAggregator();
        }
    }

    public static void sortVariablesByCounter(List<VariableInstance> variableInstances, List<VariableInstance> counterVariableInstances) {
        if (counterVariableInstances == null || counterVariableInstances.isEmpty()) {
            return;
        }
        Map<String, Integer> sortOrder = new HashMap<>();
        for (VariableInstance counterVariable : counterVariableInstances) {
            Object value = counterVariable.getValue();
            String[] values = value.toString().split(COUNTER_VAR_VALUE_SEPARATOR);
            String variableInstanceId = values[0];
            int order = Integer.parseInt(values[1]);
            sortOrder.put(variableInstanceId, order);
        }

        variableInstances.sort(Comparator.comparingInt(o -> sortOrder.getOrDefault(o.getId(), 0)));
    }

    public static Map<String, VariableAggregationDefinition> groupAggregationsByTarget(VariableScope scope,
            Collection<VariableAggregationDefinition> aggregations, CmmnEngineConfiguration cmmnEngineConfiguration) {
        Map<String, VariableAggregationDefinition> aggregationsByTarget = new HashMap<>();

        for (VariableAggregationDefinition aggregation : aggregations) {
            String targetVarName = getAggregationTargetVarName(aggregation, scope, cmmnEngineConfiguration);
            aggregationsByTarget.put(targetVarName, aggregation);
        }
        return aggregationsByTarget;
    }

    public static String getAggregationTargetVarName(VariableAggregationDefinition aggregation, VariableScope parentScope,
            CmmnEngineConfiguration cmmnEngineConfiguration) {
        String targetVarName = null;
        if (StringUtils.isNotEmpty(aggregation.getTargetExpression())) {
            Object value = cmmnEngineConfiguration.getExpressionManager()
                    .createExpression(aggregation.getTargetExpression())
                    .getValue(parentScope);
            if (value != null) {
                targetVarName = value.toString();
            }
        } else if (StringUtils.isNotEmpty(aggregation.getTarget())) {
            targetVarName = aggregation.getTarget();
        }
        return targetVarName;
    }
}
