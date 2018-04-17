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
package org.flowable.engine.compatibility;

import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.form.StartFormData;
import org.flowable.engine.impl.persistence.deploy.ProcessDefinitionCacheEntry;
import org.flowable.engine.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.engine.impl.repository.DeploymentBuilderImpl;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Attachment;
import org.flowable.engine.task.Comment;
import org.flowable.job.api.Job;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.api.persistence.entity.VariableInstance;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public interface Flowable5CompatibilityHandler {

    ProcessDefinition getProcessDefinition(String processDefinitionId);

    ProcessDefinition getProcessDefinitionByKey(String processDefinitionKey);

    org.flowable.bpmn.model.Process getProcessDefinitionProcessObject(String processDefinitionId);

    BpmnModel getProcessDefinitionBpmnModel(String processDefinitionId);

    ObjectNode getProcessDefinitionInfo(String processDefinitionId);

    ProcessDefinitionCacheEntry resolveProcessDefinition(ProcessDefinition processDefinition);

    boolean isProcessDefinitionSuspended(String processDefinitionId);

    void addCandidateStarter(String processDefinitionId, String userId, String groupId);

    void deleteCandidateStarter(String processDefinitionId, String userId, String groupId);

    void suspendProcessDefinition(String processDefinitionId, String processDefinitionKey, boolean suspendProcessInstances, Date suspensionDate, String tenantId);

    void activateProcessDefinition(String processDefinitionId, String processDefinitionKey, boolean activateProcessInstances, Date activationDate, String tenantId);

    void setProcessDefinitionCategory(String processDefinitionId, String category);

    Deployment deploy(DeploymentBuilderImpl deploymentBuilder);

    void setDeploymentCategory(String deploymentId, String category);

    void changeDeploymentTenantId(String deploymentId, String newTenantId);

    void deleteDeployment(String deploymentId, boolean cascade);

    ProcessInstance startProcessInstance(String processDefinitionKey, String processDefinitionId, Map<String, Object> variables, Map<String, Object> transientVariables,
            String businessKey, String tenantId, String processInstanceName);

    ProcessInstance startProcessInstanceByMessage(String messageName, Map<String, Object> variables, Map<String, Object> transientVariables, String businessKey, String tenantId);
    
    ProcessInstance getProcessInstance(String processInstanceId);
    
    void setProcessInstanceName(String processInstanceId, String processInstanceName);

    Object getExecutionVariable(String executionId, String variableName, boolean isLocal);

    VariableInstance getExecutionVariableInstance(String executionId, String variableName, boolean isLocal);

    Map<String, Object> getExecutionVariables(String executionId, Collection<String> variableNames, boolean isLocal);

    Map<String, VariableInstance> getExecutionVariableInstances(String executionId, Collection<String> variableNames, boolean isLocal);

    void setExecutionVariables(String executionId, Map<String, ? extends Object> variables, boolean isLocal);

    void removeExecutionVariables(String executionId, Collection<String> variableNames, boolean isLocal);

    void updateBusinessKey(String processInstanceId, String businessKey);

    void suspendProcessInstance(String processInstanceId);

    void activateProcessInstance(String processInstanceId);

    void addIdentityLinkForProcessInstance(String processInstanceId, String userId, String groupId, String identityLinkType);

    void deleteIdentityLinkForProcessInstance(String processInstanceId, String userId, String groupId, String identityLinkType);

    void deleteProcessInstance(String processInstanceId, String deleteReason);

    void deleteHistoricProcessInstance(String processInstanceId);

    void completeTask(TaskEntity taskEntity, Map<String, Object> variables, boolean localScope);

    void completeTask(TaskEntity taskEntity, Map<String, Object> variables, Map<String, Object> transientVariables);

    void claimTask(String taskId, String userId);

    void setTaskVariables(String taskId, Map<String, ? extends Object> variables, boolean isLocal);

    void removeTaskVariables(String taskId, Collection<String> variableNames, boolean isLocal);

    void setTaskDueDate(String taskId, Date dueDate);

    void setTaskPriority(String taskId, int priority);

    void deleteTask(String taskId, String deleteReason, boolean cascade);

    void deleteHistoricTask(String taskId);

    StartFormData getStartFormData(String processDefinitionId);

    String getFormKey(String processDefinitionId, String taskDefinitionKey);

    Object getRenderedStartForm(String processDefinitionId, String formEngineName);

    ProcessInstance submitStartFormData(String processDefinitionId, String businessKey, Map<String, String> properties);

    void submitTaskFormData(String taskId, Map<String, String> properties, boolean completeTask);

    void saveTask(TaskEntity task);

    void addIdentityLink(String taskId, String identityId, int identityIdType, String identityType);

    void deleteIdentityLink(String taskId, String userId, String groupId, String identityLinkType);

    Comment addComment(String taskId, String processInstanceId, String type, String message);

    void deleteComment(String commentId, String taskId, String processInstanceId);

    Attachment createAttachment(String attachmentType, String taskId, String processInstanceId, String attachmentName, String attachmentDescription, InputStream content, String url);

    void saveAttachment(Attachment attachment);

    void deleteAttachment(String attachmentId);

    void trigger(String executionId, Map<String, Object> processVariables, Map<String, Object> transientVariables);

    void messageEventReceived(String messageName, String executionId, Map<String, Object> processVariables, boolean async);

    void signalEventReceived(String signalName, String executionId, Map<String, Object> processVariables, boolean async, String tenantId);

    void signalEventReceived(SignalEventSubscriptionEntity signalEventSubscriptionEntity, Object payload, boolean async);

    void executeJob(Job job);

    void executeJobWithLockAndRetry(Job job);

    void handleFailedJob(Job job, Throwable exception);

    void deleteJob(String jobId);

    void leaveExecution(DelegateExecution execution);

    void propagateError(BpmnError bpmnError, DelegateExecution execution);

    boolean mapException(Exception camelException, DelegateExecution execution, List<MapExceptionEntry> mapExceptions);

    Map<String, Object> getVariables(ProcessInstance processInstance);

    Object getScriptingEngineValue(String payloadExpressionValue, String languageValue, DelegateExecution execution);

    void throwErrorEvent(FlowableEvent event);

    void setClock(Clock clock);

    void resetClock();

    Object getRawProcessEngine();

    Object getRawProcessConfiguration();

    Object getRawCommandExecutor();

    ProcessEngineConfiguration getFlowable6ProcessEngineConfiguration();

    void setFlowable6ProcessEngineConfiguration(ProcessEngineConfiguration processEngineConfiguration);

    Object getCamelContextObject(String camelContextValue);

    void setJobProcessor(List<Object> flowable5JobProcessors);

}
