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

package org.flowable.engine.impl.bpmn.behavior;

import static org.flowable.engine.impl.bpmn.helper.DynamicPropertyUtil.getActiveValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.delegate.SubProcessActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.EntityLinkUtil;
import org.flowable.engine.impl.util.IOParameterUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.interceptor.StartSubProcessInstanceAfterContext;
import org.flowable.engine.interceptor.StartSubProcessInstanceBeforeContext;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Implementation of the BPMN 2.0 call activity (limited currently to calling a subprocess and not (yet) a global task).
 *
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class CallActivityBehavior extends AbstractBpmnActivityBehavior implements SubProcessActivityBehavior {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallActivityBehavior.class);

    private static final long serialVersionUID = 1L;

    private static final String EXPRESSION_REGEX = "\\$+\\{+.+\\}";
    public static final String CALLED_ELEMENT_TYPE_KEY = "key";
    public static final String CALLED_ELEMENT_TYPE_ID = "id";

    protected CallActivity callActivity;
    protected String calledElementType;
    protected Boolean fallbackToDefaultTenant;
    protected List<MapExceptionEntry> mapExceptions;

    public CallActivityBehavior(CallActivity callActivity) {
        this.callActivity = callActivity;
        this.calledElementType = callActivity.getCalledElementType();
        this.mapExceptions = callActivity.getMapExceptions();
        this.fallbackToDefaultTenant = callActivity.getFallbackToDefaultTenant();
    }

    @Override
    public void execute(DelegateExecution execution) {

        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        CallActivity callActivity = (CallActivity) executionEntity.getCurrentFlowElement();
        
        CommandContext commandContext = CommandContextUtil.getCommandContext();

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);

        ProcessDefinition processDefinition = getProcessDefinition(execution, callActivity, processEngineConfiguration);

        // Get model from cache
        Process subProcess = ProcessDefinitionUtil.getProcess(processDefinition.getId());
        if (subProcess == null) {
            throw new FlowableException("Cannot start a sub process instance. Process model " + processDefinition.getName() + " (id = " + processDefinition.getId() + ") could not be found");
        }

        FlowElement initialFlowElement = subProcess.getInitialFlowElement();
        if (initialFlowElement == null) {
            throw new FlowableException("No start element found for process definition " + processDefinition.getId());
        }

        // Do not start a process instance if the process definition is suspended
        if (ProcessDefinitionUtil.isProcessDefinitionSuspended(processDefinition.getId())) {
            throw new FlowableException("Cannot start process instance. Process definition " + processDefinition.getName() + " (id = " + processDefinition.getId() + ") is suspended");
        }

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();

        String businessKey = null;
        if (!StringUtils.isEmpty(callActivity.getBusinessKey())) {
            Expression expression = expressionManager.createExpression(callActivity.getBusinessKey());
            businessKey = expression.getValue(execution).toString();

        } else if (callActivity.isInheritBusinessKey()) {
            ExecutionEntity processInstance = executionEntityManager.findById(execution.getProcessInstanceId());
            businessKey = processInstance.getBusinessKey();
        }

        StartSubProcessInstanceBeforeContext instanceBeforeContext = new StartSubProcessInstanceBeforeContext(businessKey, null,
                callActivity.getProcessInstanceName(), new HashMap<>(), new HashMap<>(), executionEntity, callActivity.getInParameters(),
                callActivity.isInheritVariables(), initialFlowElement.getId(), initialFlowElement, subProcess, processDefinition);
        
        if (processEngineConfiguration.getStartProcessInstanceInterceptor() != null) {
            processEngineConfiguration.getStartProcessInstanceInterceptor().beforeStartSubProcessInstance(instanceBeforeContext);
        }

        ExecutionEntity subProcessInstance = processEngineConfiguration.getExecutionEntityManager().createSubprocessInstance(
                        instanceBeforeContext.getProcessDefinition(), instanceBeforeContext.getCallActivityExecution(), 
                        instanceBeforeContext.getBusinessKey(), instanceBeforeContext.getInitialActivityId());

        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(
                    FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_CREATED, subProcessInstance),
                    processEngineConfiguration.getEngineCfgKey());
        }

        // process template-defined data objects
        subProcessInstance.setVariables(processDataObjects(subProcess.getDataObjects()));

        if (instanceBeforeContext.isInheritVariables()) {

            Map<String, Object> executionVariables = execution.getVariables();
            Map<String, Object> transientVariables = execution.getTransientVariables();
            for (Map.Entry<String, Object> entry : executionVariables.entrySet()) {

                // The executionVariables contain all variables, including the transient variables.
                // Hence why that map is iterated and the transient variables are split off
                String variableName = entry.getKey();
                if (transientVariables.containsKey(variableName)) {
                    instanceBeforeContext.getTransientVariables().put(variableName, entry.getValue());

                } else {
                    instanceBeforeContext.getVariables().put(variableName, entry.getValue());

                }
            }

        }

        List<IOParameter> inParameters = instanceBeforeContext.getInParameters();
        if (!inParameters.isEmpty()) {
            Map<String, Object> variables = instanceBeforeContext.getVariables();
            // copy process variables
            IOParameterUtil.processInParameters(inParameters, execution, variables::put, variables::put, expressionManager);
        }

        if (!instanceBeforeContext.getVariables().isEmpty()) {
            initializeVariables(subProcessInstance, instanceBeforeContext.getVariables());
        }

        if (!instanceBeforeContext.getTransientVariables().isEmpty()) {
            initializeTransientVariables(subProcessInstance, instanceBeforeContext.getTransientVariables());
        }
        
        // Process instance name is resolved after setting the variables on the process instance, so they can be used in the expression
        String processInstanceName = null;
        if (StringUtils.isNotEmpty(instanceBeforeContext.getProcessInstanceName())) {
            Expression processInstanceNameExpression = expressionManager.createExpression(instanceBeforeContext.getProcessInstanceName());
            processInstanceName = processInstanceNameExpression.getValue(subProcessInstance).toString();
            subProcessInstance.setName(processInstanceName);
        }

        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_INITIALIZED, subProcessInstance),
                    processEngineConfiguration.getEngineCfgKey());
        }
        
        if (processEngineConfiguration.isEnableEntityLinks()) {
            EntityLinkUtil.createEntityLinks(execution.getProcessInstanceId(), executionEntity.getId(), callActivity.getId(),
                    subProcessInstance.getId(), ScopeTypes.BPMN);
        }

        if (StringUtils.isNotEmpty(callActivity.getProcessInstanceIdVariableName())) {
            Expression expression = expressionManager.createExpression(callActivity.getProcessInstanceIdVariableName());
            String idVariableName = (String) expression.getValue(execution);
            if (StringUtils.isNotEmpty(idVariableName)) {
                execution.setVariable(idVariableName, subProcessInstance.getId());
            }
        }

        // Create the first execution that will visit all the process definition elements
        ExecutionEntity subProcessInitialExecution = executionEntityManager.createChildExecution(subProcessInstance);
        subProcessInitialExecution.setCurrentFlowElement(instanceBeforeContext.getInitialFlowElement());

        if (processEngineConfiguration.getStartProcessInstanceInterceptor() != null) {
            StartSubProcessInstanceAfterContext instanceAfterContext = new StartSubProcessInstanceAfterContext(subProcessInstance, subProcessInitialExecution,
                instanceBeforeContext.getVariables(), instanceBeforeContext.getTransientVariables(), instanceBeforeContext.getCallActivityExecution(),
                instanceBeforeContext.getInParameters(), instanceBeforeContext.getInitialFlowElement(), instanceBeforeContext.getProcess(),
                instanceBeforeContext.getProcessDefinition());

            processEngineConfiguration.getStartProcessInstanceInterceptor().afterStartSubProcessInstance(instanceAfterContext);
        }

        processEngineConfiguration.getActivityInstanceEntityManager().recordSubProcessInstanceStart(executionEntity, subProcessInstance);

        CommandContextUtil.getAgenda().planContinueProcessOperation(subProcessInitialExecution);

        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            Map<String, Object> allVariables = new HashMap<>();
            allVariables.putAll(instanceBeforeContext.getVariables());
            allVariables.putAll(instanceBeforeContext.getTransientVariables());
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createProcessStartedEvent(subProcessInitialExecution, allVariables, false),
                    processEngineConfiguration.getEngineCfgKey());
        }
        
    }

    protected ProcessDefinition getProcessDefinition(DelegateExecution execution, CallActivity callActivity, ProcessEngineConfigurationImpl processEngineConfiguration) {
        ProcessDefinition processDefinition = switch (StringUtils.isNotEmpty(calledElementType) ? calledElementType : CALLED_ELEMENT_TYPE_KEY) {
            case CALLED_ELEMENT_TYPE_ID -> getProcessDefinitionById(execution, processEngineConfiguration);
            case CALLED_ELEMENT_TYPE_KEY -> getProcessDefinitionByKey(execution, callActivity.isSameDeployment(), processEngineConfiguration);
            default -> throw new FlowableException("Unrecognized calledElementType [" + calledElementType + "] in " + execution);
        };
        return processDefinition;
    }

    @Override
    public void completing(DelegateExecution execution, DelegateExecution subProcessInstance) throws Exception {
        // only data. no control flow available on this execution.

        ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration().getExpressionManager();

        // copy process variables
        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        CallActivity callActivity = (CallActivity) executionEntity.getCurrentFlowElement();

        List<IOParameter> outParameters = callActivity.getOutParameters();
        if (!outParameters.isEmpty()) {
            BiConsumer<String, Object> variableConsumer = (variableName, value) -> {
                if (callActivity.isUseLocalScopeForOutParameters()) {
                    executionEntity.setVariableLocal(variableName, value);
                } else {
                    executionEntity.setVariable(variableName, value);
                }
            };

            IOParameterUtil.processOutParameters(outParameters, subProcessInstance, variableConsumer, variableConsumer, expressionManager);
        }
    }

    @Override
    public void completed(DelegateExecution execution) throws Exception {
        // only control flow. no sub process instance data available

        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        if (executionEntity.isSuspended() || ProcessDefinitionUtil.isProcessDefinitionSuspended(execution.getProcessDefinitionId())) {
            throw new FlowableException("Cannot complete process instance. Parent process instance " + executionEntity + " is suspended");
        }

        leave(execution);
    }

    protected ProcessDefinition getProcessDefinitionById(DelegateExecution execution, ProcessEngineConfigurationImpl processEngineConfiguration) {
        return CommandContextUtil.getProcessEngineConfiguration().getDeploymentManager()
            .findDeployedProcessDefinitionById(getCalledElementValue(execution, processEngineConfiguration));
    }

    protected ProcessDefinition getProcessDefinitionByKey(DelegateExecution execution, boolean isSameDeployment, ProcessEngineConfigurationImpl processEngineConfiguration) {
        String processDefinitionKey = getCalledElementValue(execution, processEngineConfiguration);
        String tenantId = execution.getTenantId();

        ProcessDefinitionEntityManager processDefinitionEntityManager = Context.getProcessEngineConfiguration().getProcessDefinitionEntityManager();
        ProcessDefinitionEntity processDefinition;

        if (isSameDeployment) {
            String deploymentId = ProcessDefinitionUtil.getProcessDefinition(execution.getProcessDefinitionId()).getDeploymentId();
            if (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                processDefinition = processDefinitionEntityManager.findProcessDefinitionByDeploymentAndKey(deploymentId, processDefinitionKey);
            } else {
                processDefinition = processDefinitionEntityManager.findProcessDefinitionByDeploymentAndKeyAndTenantId(deploymentId, processDefinitionKey, tenantId);
            }

            if (processDefinition != null) {
                return processDefinition;
            }
        }

        if (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
            processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKey(processDefinitionKey);
        } else {
            processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, tenantId);
            if (processDefinition == null && ((this.fallbackToDefaultTenant != null && this.fallbackToDefaultTenant) || processEngineConfiguration.isFallbackToDefaultTenant())) {

                String defaultTenant = processEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(tenantId, ScopeTypes.BPMN, processDefinitionKey);
                if (StringUtils.isNotEmpty(defaultTenant)) {
                    processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKeyAndTenantId(
                                    processDefinitionKey, defaultTenant);
                } else {
                    processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKey(processDefinitionKey);
                }
            }
        }

        if (processDefinition == null) {
            throw new FlowableObjectNotFoundException("Process definition " + processDefinitionKey + " was not found in sameDeployment["+ isSameDeployment +
                "] tenantId["+ tenantId+ "] fallbackToDefaultTenant["+ this.fallbackToDefaultTenant + "]");
        }
        return processDefinition;
    }

    protected String getCalledElementValue(DelegateExecution execution, ProcessEngineConfigurationImpl processEngineConfiguration) {
        String calledElementValue = callActivity.getCalledElement();
        if (Context.getProcessEngineConfiguration().isEnableProcessDefinitionInfoCache()){
            ObjectNode taskElementProperties = BpmnOverrideContext
                    .getBpmnOverrideElementProperties(callActivity.getId(), execution.getProcessDefinitionId());
            calledElementValue = getActiveValue(callActivity.getCalledElement(), DynamicBpmnConstants.CALL_ACTIVITY_CALLED_ELEMENT, taskElementProperties);
        }
        if (StringUtils.isNotEmpty(calledElementValue) && calledElementValue.matches(EXPRESSION_REGEX)) {
            calledElementValue = (String) processEngineConfiguration.getExpressionManager().createExpression(calledElementValue).getValue(execution);
        }
        return calledElementValue;
    }

    protected Map<String, Object> processDataObjects(Collection<ValuedDataObject> dataObjects) {
        Map<String, Object> variablesMap = new HashMap<>();
        // convert data objects to process variables
        if (dataObjects != null) {
            variablesMap = new HashMap<>(dataObjects.size());
            for (ValuedDataObject dataObject : dataObjects) {
                variablesMap.put(dataObject.getName(), dataObject.getValue());
            }
        }
        return variablesMap;
    }

    // Allow a subclass to override how variables are initialized.
    protected void initializeVariables(ExecutionEntity subProcessInstance, Map<String, Object> variables) {
        subProcessInstance.setVariables(variables);
    }

    // Allow a subclass to override how variables are initialized.
    protected void initializeTransientVariables(ExecutionEntity subProcessInstance, Map<String, Object> transientVariables) {
        subProcessInstance.setTransientVariables(transientVariables);
    }
}
