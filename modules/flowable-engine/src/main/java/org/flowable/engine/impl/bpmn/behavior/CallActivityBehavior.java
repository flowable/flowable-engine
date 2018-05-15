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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.delegate.SubProcessActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * Implementation of the BPMN 2.0 call activity (limited currently to calling a subprocess and not (yet) a global task).
 *
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class CallActivityBehavior extends AbstractBpmnActivityBehavior implements SubProcessActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected String processDefinitonKey;
    protected Expression processDefinitionExpression;
    protected List<MapExceptionEntry> mapExceptions;

    public CallActivityBehavior(String processDefinitionKey, List<MapExceptionEntry> mapExceptions) {
        this.processDefinitonKey = processDefinitionKey;
        this.mapExceptions = mapExceptions;
    }

    public CallActivityBehavior(Expression processDefinitionExpression, List<MapExceptionEntry> mapExceptions) {
        this.processDefinitionExpression = processDefinitionExpression;
        this.mapExceptions = mapExceptions;
    }

    @Override
    public void execute(DelegateExecution execution) {

        String finalProcessDefinitonKey = null;
        if (processDefinitionExpression != null) {
            finalProcessDefinitonKey = (String) processDefinitionExpression.getValue(execution);
        } else {
            finalProcessDefinitonKey = processDefinitonKey;
        }

        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        CallActivity callActivity = (CallActivity) executionEntity.getCurrentFlowElement();

        ProcessDefinition processDefinition = findProcessDefinition(finalProcessDefinitonKey, execution.getProcessDefinitionId(), execution.getTenantId(), callActivity.isSameDeployment());

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

        CommandContext commandContext = CommandContextUtil.getCommandContext();

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
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

        ExecutionEntity subProcessInstance = CommandContextUtil.getExecutionEntityManager(commandContext).createSubprocessInstance(
                processDefinition, executionEntity, businessKey, initialFlowElement.getId());
        CommandContextUtil.getHistoryManager(commandContext).recordSubProcessInstanceStart(executionEntity, subProcessInstance);

        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (eventDispatcher.isEnabled()) {
            CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
                    FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_CREATED, subProcessInstance));
        }

        // process template-defined data objects
        subProcessInstance.setVariables(processDataObjects(subProcess.getDataObjects()));

        Map<String, Object> variables = new HashMap<>();

        if (callActivity.isInheritVariables()) {
            Map<String, Object> executionVariables = execution.getVariables();
            for (Map.Entry<String, Object> entry : executionVariables.entrySet()) {
                variables.put(entry.getKey(), entry.getValue());
            }
        }
        
        // copy process variables
        for (IOParameter ioParameter : callActivity.getInParameters()) {
            Object value = null;
            if (StringUtils.isNotEmpty(ioParameter.getSourceExpression())) {
                Expression expression = expressionManager.createExpression(ioParameter.getSourceExpression().trim());
                value = expression.getValue(execution);

            } else {
                value = execution.getVariable(ioParameter.getSource());
            }
            variables.put(ioParameter.getTarget(), value);
        }

        if (!variables.isEmpty()) {
            initializeVariables(subProcessInstance, variables);
        }
        
        // Process instance name is resolved after setting the variables on the process instance, so they can be used in the expression
        String processInstanceName = null;
        if (StringUtils.isNotEmpty(callActivity.getProcessInstanceName())) {
            Expression processInstanceNameExpression = expressionManager.createExpression(callActivity.getProcessInstanceName());
            processInstanceName = processInstanceNameExpression.getValue(subProcessInstance).toString();
            subProcessInstance.setName(processInstanceName);
        }

        if (eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_INITIALIZED, subProcessInstance));
        }

        // Create the first execution that will visit all the process definition elements
        ExecutionEntity subProcessInitialExecution = executionEntityManager.createChildExecution(subProcessInstance);
        subProcessInitialExecution.setCurrentFlowElement(initialFlowElement);

        CommandContextUtil.getAgenda().planContinueProcessOperation(subProcessInitialExecution);

        if (eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createProcessStartedEvent(subProcessInitialExecution, variables, false));
        }
    }

    @Override
    public void completing(DelegateExecution execution, DelegateExecution subProcessInstance) throws Exception {
        // only data. no control flow available on this execution.

        ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration().getExpressionManager();

        // copy process variables
        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        CallActivity callActivity = (CallActivity) executionEntity.getCurrentFlowElement();
        for (IOParameter ioParameter : callActivity.getOutParameters()) {
            Object value = null;
            if (StringUtils.isNotEmpty(ioParameter.getSourceExpression())) {
                Expression expression = expressionManager.createExpression(ioParameter.getSourceExpression().trim());
                value = expression.getValue(subProcessInstance);

            } else {
                value = subProcessInstance.getVariable(ioParameter.getSource());
            }

            if (callActivity.isUseLocalScopeForOutParameters()) {
                execution.setVariableLocal(ioParameter.getTarget(), value);
            } else {
                execution.setVariable(ioParameter.getTarget(), value);
            }
        }
    }

    @Override
    public void completed(DelegateExecution execution) throws Exception {
        // only control flow. no sub process instance data available
        leave(execution);
    }

    // Allow subclass to determine which version of a process to start.
    protected ProcessDefinition findProcessDefinition(String processDefinitionKey, String processDefinitionId, String tenantId, boolean sameDeployment) {
        if (sameDeployment) {
            String deploymentId = ProcessDefinitionUtil.getProcessDefinition(processDefinitionId).getDeploymentId();
            ProcessDefinitionEntityManager processDefinitionEntityManager = Context.getProcessEngineConfiguration().getProcessDefinitionEntityManager();
            ProcessDefinitionEntity processDefinitionByDeploymentAndKey = null;
            if (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                processDefinitionByDeploymentAndKey = processDefinitionEntityManager.findProcessDefinitionByDeploymentAndKey(deploymentId, processDefinitionKey);
            } else {
                processDefinitionByDeploymentAndKey = processDefinitionEntityManager.findProcessDefinitionByDeploymentAndKeyAndTenantId(deploymentId, processDefinitionKey, tenantId);
            }
            
            if (processDefinitionByDeploymentAndKey != null) {
                return processDefinitionByDeploymentAndKey;
            }
        }

        if (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
            return CommandContextUtil.getProcessEngineConfiguration().getDeploymentManager().findDeployedLatestProcessDefinitionByKey(processDefinitionKey);
        } else {
            return CommandContextUtil.getProcessEngineConfiguration().getDeploymentManager().findDeployedLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, tenantId);
        }
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

    public void setProcessDefinitonKey(String processDefinitonKey) {
        this.processDefinitonKey = processDefinitonKey;
    }

    public String getProcessDefinitonKey() {
        return processDefinitonKey;
    }
}
