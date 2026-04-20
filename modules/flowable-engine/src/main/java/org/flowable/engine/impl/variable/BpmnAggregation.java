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
package org.flowable.engine.impl.variable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.VariableAggregationDefinition;
import org.flowable.bpmn.model.VariableAggregationDefinitions;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.variable.VariableAggregator;
import org.flowable.engine.delegate.variable.VariableAggregatorContext;
import org.flowable.engine.impl.bpmn.helper.ClassDelegateUtil;
import org.flowable.engine.impl.bpmn.helper.DelegateExpressionUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.delegate.BaseVariableAggregatorContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Filip Hrisafov
 */
public class BpmnAggregation {

    public static final String COUNTER_VAR_PREFIX = "__flowableCounter__";
    public static final String COUNTER_VAR_VALUE_SEPARATOR = "###";

    protected final String executionId;

    public BpmnAggregation(String executionId) {
        this.executionId = executionId;
    }

    public String getExecutionId() {
        return executionId;
    }

    /**
     * Create an aggregated variable instance for a completed execution.
     *
     * @param execution the execution which completed
     * @param parentExecution the parent execution
     * @param aggregation the aggregation definition
     * @param processEngineConfiguration the process engine configuration
     * @return the created variables (not yet saved), or {@code null} if no name could be resolved for the variable
     */
    public static VariableInstanceEntity aggregateComplete(DelegateExecution execution, DelegateExecution parentExecution,
            VariableAggregationDefinition aggregation, ProcessEngineConfigurationImpl processEngineConfiguration) {
        return aggregate(execution, parentExecution, BaseVariableAggregatorContext.complete(aggregation), processEngineConfiguration);
    }

    /**
     * Create an aggregated variable instance for the given aggregation context.
     *
     * @param execution the execution
     * @param parentExecution the parent execution
     * @param aggregationContext the aggregation context
     * @param processEngineConfiguration the process engine configuration
     * @return the created variables (not yet saved), or {@code null} if no name could be resolved for the variable
     */
    public static VariableInstanceEntity aggregate(DelegateExecution execution, DelegateExecution parentExecution,
            VariableAggregatorContext aggregationContext, ProcessEngineConfigurationImpl processEngineConfiguration) {
        VariableAggregationDefinition aggregation = aggregationContext.getDefinition();
        VariableAggregator aggregator = resolveVariableAggregator(aggregation, execution);
        String targetVarName = getAggregationTargetVarName(aggregation, execution, processEngineConfiguration);

        if (targetVarName != null) {
            return aggregate(execution, parentExecution, aggregationContext, processEngineConfiguration, aggregator, targetVarName);
        }

        return null;
    }

    /**
     * @param execution the execution
     * @param parentExecution the parent execution
     * @param aggregationContext the aggregation context
     * @param processEngineConfiguration the process engine configuration
     * @param aggregator the aggregator that should be used to perform the aggregation
     * @param targetVarName the name of the variable, never {@code null}
     * @return the created variables (not yet saved), or {@code null} if no name could be resolved for the variable
     */
    public static VariableInstanceEntity aggregate(DelegateExecution execution, DelegateExecution parentExecution,
            VariableAggregatorContext aggregationContext, ProcessEngineConfigurationImpl processEngineConfiguration, VariableAggregator aggregator,
            String targetVarName) {

        VariableServiceConfiguration variableServiceConfiguration = processEngineConfiguration.getVariableServiceConfiguration();

        Object aggregatedValue = aggregator.aggregateSingleVariable(execution, aggregationContext);

        String processInstanceId = execution.getProcessInstanceId();
        String parentExecutionId = parentExecution.getId();

        return createScopedVariableAggregationVariableInstance(execution.getTenantId(), targetVarName, processInstanceId, parentExecutionId,
                aggregatedValue, variableServiceConfiguration);
    }

    /**
     * Aggregate an overview value for the execution and variable name
     *
     * @param parentExecutionId the execution for which the overview needs to be done
     * @param targetVarName the name of the variable
     * @return the aggregated variable value
     */
    public static Object aggregateOverview(String parentExecutionId, String targetVarName, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExecutionEntity parentExecution = processEngineConfiguration.getExecutionEntityManager()
                .findById(parentExecutionId);

        if (parentExecution.isMultiInstanceRoot()) {
            return aggregateOverviewForMultiInstance(parentExecution, targetVarName, processEngineConfiguration);
        }

        // TODO aggregation overview for non multi instance
        return null;

    }

    /**
     * Create an overview value for a multi instance execution
     *
     * @param parentExecution the multi instance root execution
     * @param targetVarName the name of the variable
     * @param processEngineConfiguration the process engine configuration
     * @return the overview value for the multi instance execution
     */
    public static Object aggregateOverviewForMultiInstance(ExecutionEntity parentExecution, String targetVarName,
            ProcessEngineConfigurationImpl processEngineConfiguration) {
        FlowElement currentFlowElement = parentExecution.getCurrentFlowElement();
        if (!(currentFlowElement instanceof Activity activity)) {
            // This should never happen as the parent execution is a multi instance root, but we are being extra safe
            return null;
        }

        MultiInstanceLoopCharacteristics loopCharacteristics = activity.getLoopCharacteristics();
        if (loopCharacteristics == null) {
            // This should never happen as the parent execution is a multi instance root, but we are being extra safe
            return null;
        }

        VariableAggregationDefinitions aggregations = loopCharacteristics.getAggregations();
        if (aggregations == null) {
            // This should never happen as the parent execution is a multi instance root and we are doing an aggregation, but we are being extra safe
            return null;
        }

        VariableServiceConfiguration variableServiceConfiguration = processEngineConfiguration.getVariableServiceConfiguration();
        VariableService variableService = variableServiceConfiguration.getVariableService();
        // Find all variable instances for already completed children flows of the multi instance execution
        List<VariableInstanceEntity> instances = variableService.createInternalVariableInstanceQuery()
                .subScopeId(parentExecution.getId())
                .scopeType(ScopeTypes.BPMN_VARIABLE_AGGREGATION)
                .names(Arrays.asList(targetVarName, COUNTER_VAR_PREFIX + targetVarName))
                .list();

        String elementIndexVariable = StringUtils.defaultIfBlank(loopCharacteristics.getElementIndexVariable(), "loopCounter");

        Map<String, VariableAggregationDefinition> aggregationsByTarget = groupAggregationsByTarget(parentExecution, aggregations.getOverviewAggregations(), processEngineConfiguration);
        VariableAggregationDefinition aggregation = aggregationsByTarget.get(targetVarName);
        VariableAggregator aggregator = resolveVariableAggregator(aggregation, parentExecution);

        VariableAggregatorContext aggregationContext = BaseVariableAggregatorContext.overview(aggregation);

        for (ExecutionEntity childExecution : parentExecution.getExecutions()) {
            // We need to create overview values for every single active child execution
            if (childExecution.isActive() && !(childExecution.getCurrentFlowElement() instanceof BoundaryEvent)) {

                VariableInstanceEntity aggregatedVarInstance = aggregate(childExecution, parentExecution,
                        aggregationContext, processEngineConfiguration, aggregator, targetVarName);
                aggregatedVarInstance.setId(childExecution.getId() + targetVarName);
                instances.add(aggregatedVarInstance);

                Integer elementIndexValue = childExecution.getVariable(elementIndexVariable, Integer.class);
                if (elementIndexValue == null) {
                    elementIndexValue = 0;
                }
                String counterValue = aggregatedVarInstance.getId() + COUNTER_VAR_VALUE_SEPARATOR + elementIndexValue;
                VariableInstanceEntity counterVarInstance = createScopedVariableAggregationVariableInstance(childExecution.getTenantId(), COUNTER_VAR_PREFIX + targetVarName,
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

            return aggregator.aggregateMultiVariables(parentExecution, targetVariableInstances, aggregationContext);
        }

        return null;
    }

    public static VariableInstanceEntity createScopedVariableAggregationVariableInstance(String tenantId, String varName, String scopeId, String subScopeId, Object value,
            VariableServiceConfiguration variableServiceConfiguration) {

        VariableService variableService = variableServiceConfiguration.getVariableService();

        VariableInstanceEntity variableInstance = variableService.createVariableInstance(varName);
        variableInstance.setScopeId(scopeId);
        variableInstance.setSubScopeId(subScopeId);
        variableInstance.setScopeType(ScopeTypes.BPMN_VARIABLE_AGGREGATION);
        variableServiceConfiguration.getVariableInstanceValueModifier().setVariableValue(variableInstance, value, tenantId);
        return variableInstance;
    }

    public static Map<String, List<VariableInstance>> groupVariableInstancesByName(List<? extends VariableInstance> instances) {
        return instances.stream().collect(Collectors.groupingBy(VariableInstance::getName));
    }

    public static VariableAggregator resolveVariableAggregator(VariableAggregationDefinition aggregation, DelegateExecution execution) {
        if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(aggregation.getImplementationType())) {
            Object delegate = ClassDelegateUtil.instantiateDelegate(aggregation.getImplementation(), null);
            if (delegate instanceof VariableAggregator) {
                return (VariableAggregator) delegate;
            }

            throw new FlowableIllegalArgumentException("Class " + aggregation.getImplementation() + " does not implement " + VariableAggregator.class);
        } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(aggregation.getImplementationType())) {
            Object delegate = DelegateExpressionUtil.resolveDelegateExpression(
                    CommandContextUtil.getProcessEngineConfiguration().getExpressionManager().createExpression(aggregation.getImplementation()), execution);

            if (delegate instanceof VariableAggregator) {
                return (VariableAggregator) delegate;
            }

            throw new FlowableIllegalArgumentException(
                    "Delegate expression " + aggregation.getImplementation() + " did not resolve to an implementation of " + VariableAggregator.class);
        } else {
            return CommandContextUtil.getProcessEngineConfiguration().getVariableAggregator();
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

    public static Map<String, VariableAggregationDefinition> groupAggregationsByTarget(DelegateExecution multiInstanceRootExecution,
            Collection<VariableAggregationDefinition> aggregations, ProcessEngineConfigurationImpl processEngineConfiguration) {

        Map<String, VariableAggregationDefinition> aggregationsByTarget = new HashMap<>();

        for (VariableAggregationDefinition aggregation : aggregations) {
            String targetVarName = getAggregationTargetVarName(aggregation, multiInstanceRootExecution, processEngineConfiguration);
            aggregationsByTarget.put(targetVarName, aggregation);
        }
        return aggregationsByTarget;
    }

    public static String getAggregationTargetVarName(VariableAggregationDefinition aggregation, DelegateExecution execution,
            ProcessEngineConfigurationImpl processEngineConfiguration) {
        String targetVarName = null;
        if (StringUtils.isNotEmpty(aggregation.getTargetExpression())) {
            Object value = processEngineConfiguration.getExpressionManager()
                    .createExpression(aggregation.getTargetExpression())
                    .getValue(execution);
            if (value != null) {
                targetVarName = value.toString();
            }
        } else if (StringUtils.isNotEmpty(aggregation.getTarget())) {
            targetVarName = aggregation.getTarget();
        }
        return targetVarName;
    }
}
