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
package org.flowable.rest.service.api;

import java.util.Collection;
import java.util.Map;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchQuery;
import org.flowable.engine.form.FormData;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.history.HistoricDetailQuery;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.DeploymentQuery;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ModelQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ActivityInstanceQuery;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.engine.task.Attachment;
import org.flowable.engine.task.Comment;
import org.flowable.engine.task.Event;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionQuery;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.GroupQuery;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.flowable.job.api.DeadLetterJobQuery;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.HistoryJobQuery;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobQuery;
import org.flowable.job.api.SuspendedJobQuery;
import org.flowable.job.api.TimerJobQuery;
import org.flowable.rest.service.api.engine.CommentRequest;
import org.flowable.rest.service.api.engine.RestIdentityLink;
import org.flowable.rest.service.api.engine.variable.RestVariable;
import org.flowable.rest.service.api.form.SubmitFormRequest;
import org.flowable.rest.service.api.history.HistoricActivityInstanceQueryRequest;
import org.flowable.rest.service.api.history.HistoricDetailQueryRequest;
import org.flowable.rest.service.api.history.HistoricProcessInstanceQueryRequest;
import org.flowable.rest.service.api.history.HistoricTaskInstanceQueryRequest;
import org.flowable.rest.service.api.history.HistoricTaskLogEntryQueryRequest;
import org.flowable.rest.service.api.history.HistoricVariableInstanceQueryRequest;
import org.flowable.rest.service.api.identity.GroupRequest;
import org.flowable.rest.service.api.identity.UserRequest;
import org.flowable.rest.service.api.repository.ModelRequest;
import org.flowable.rest.service.api.repository.ProcessDefinitionActionRequest;
import org.flowable.rest.service.api.runtime.VariableInstanceQueryRequest;
import org.flowable.rest.service.api.runtime.process.ActivityInstanceQueryRequest;
import org.flowable.rest.service.api.runtime.process.ExecutionActionRequest;
import org.flowable.rest.service.api.runtime.process.ExecutionChangeActivityStateRequest;
import org.flowable.rest.service.api.runtime.process.ExecutionQueryRequest;
import org.flowable.rest.service.api.runtime.process.InjectActivityRequest;
import org.flowable.rest.service.api.runtime.process.ProcessInstanceCreateRequest;
import org.flowable.rest.service.api.runtime.process.ProcessInstanceQueryRequest;
import org.flowable.rest.service.api.runtime.process.ProcessInstanceUpdateRequest;
import org.flowable.rest.service.api.runtime.process.SignalEventReceivedRequest;
import org.flowable.rest.service.api.runtime.task.BulkTasksRequest;
import org.flowable.rest.service.api.runtime.task.TaskActionRequest;
import org.flowable.rest.service.api.runtime.task.TaskQueryRequest;
import org.flowable.rest.service.api.runtime.task.TaskRequest;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.task.api.history.HistoricTaskLogEntryQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.runtime.VariableInstanceQuery;

public interface BpmnRestApiInterceptor {

    void accessEngineProperties();

    void accessTaskInfoById(Task task);
    
    void accessTaskInfoWithQuery(TaskQuery taskQuery, TaskQueryRequest request);
    
    void bulkDeleteHistoricProcessInstances(Collection<String> instanceIds);

    void bulkMoveDeadLetterJobs(Collection<String> jobIds, String moveAction);

    void bulkUpdateTasks(Collection<Task> taskList, BulkTasksRequest bulkTasksRequest);

    void createTask(Task task, TaskRequest request);
    
    void updateTask(Task task, TaskRequest request);

    void deleteTask(Task task);

    void createTaskAttachment(Task task);

    void deleteTaskAttachment(Task task, Attachment attachment);

    void createTaskComment(Task task, CommentRequest comment);

    void deleteTaskComment(Task task, Comment comment);

    void deleteTaskEvent(Task task, Event event);
    
    void executeTaskAction(Task task, TaskActionRequest actionRequest);
    
    void accessTaskVariable(Task task, String variableName);

    Map<String, RestVariable> accessTaskVariables(Task task, Map<String, RestVariable> variableMap);

    void createTaskVariables(Task task, Map<String, Object> variables, RestVariable.RestVariableScope scope);

    void updateTaskVariables(Task task, Map<String, Object> variables, RestVariable.RestVariableScope scope);

    void deleteTaskVariables(Task task, Collection<String> variableNames, RestVariable.RestVariableScope scope);

    void accessTaskIdentityLinks(Task task);

    void accessTaskIdentityLink(Task task, IdentityLink identityLink);

    void deleteTaskIdentityLink(Task task, IdentityLink identityLink);

    void createTaskIdentityLink(Task task, RestIdentityLink identityLink);

    void accessExecutionInfoById(Execution execution);

    void accessExecutionVariable(Execution execution, String variableName, String scope);

    Map<String, RestVariable> accessExecutionVariables(Execution execution, Map<String, RestVariable> variables);

    void accessExecutionInfoWithQuery(ExecutionQuery executionQuery, ExecutionQueryRequest request);
    
    void doExecutionActionRequest(ExecutionActionRequest executionActionRequest);
    
    void createExecutionVariables(Execution execution, Map<String, Object> variables, RestVariable.RestVariableScope scope);

    void updateExecutionVariables(Execution execution, Map<String, Object> variables, RestVariable.RestVariableScope scope);

    void deleteExecutionVariables(Execution execution, Collection<String> variableNames, RestVariable.RestVariableScope scope);

    void accessProcessInstanceInfoById(ProcessInstance processInstance);

    void accessProcessInstanceInfoWithQuery(ProcessInstanceQuery processInstanceQuery, ProcessInstanceQueryRequest request);
    
    void createProcessInstance(ProcessInstanceBuilder processInstanceBuilder, ProcessInstanceCreateRequest request);

    void updateProcessInstance(ProcessInstance processInstance, ProcessInstanceUpdateRequest updateRequest);

    void deleteProcessInstance(ProcessInstance processInstance);
    
    void accessProcessInstanceIdentityLinks(ProcessInstance processInstance);

    void accessProcessInstanceIdentityLink(ProcessInstance processInstance, IdentityLink identityLink);

    void deleteProcessInstanceIdentityLink(ProcessInstance processInstance, IdentityLink identityLink);

    void createProcessInstanceIdentityLink(ProcessInstance processInstance, RestIdentityLink identityLink);

    void bulkDeleteProcessInstances(Collection<String> processInstances);
    
    void accessActivityInfoWithQuery(ActivityInstanceQuery activityInstanceQuery, ActivityInstanceQueryRequest request);

    void accessVariableInfoById(VariableInstance variableInstance);
    
    void accessVariableInfoWithQuery(VariableInstanceQuery variableInstanceQuery, VariableInstanceQueryRequest request);
    
    void sendSignal(SignalEventReceivedRequest signalEventReceivedRequest);
    
    void changeActivityState(ExecutionChangeActivityStateRequest changeActivityStateRequest);
    
    void migrateProcessInstance(String processInstanceId, String migrationDocument);
    
    void migrateInstancesOfProcessDefinition(ProcessDefinition processDefinition, String migrationDocument);
    
    void evaluateProcessInstanceConditionalEvents(ProcessInstance processInstance);

    void injectActivity(InjectActivityRequest injectActivityRequest);
    
    void accessEventSubscriptionById(EventSubscription eventSubscription);
    
    void accessEventSubscriptionInfoWithQuery(EventSubscriptionQuery eventSubscriptionQuery);
    
    void accessProcessDefinitionById(ProcessDefinition processDefinition);
    
    void executeProcessDefinitionAction(ProcessDefinition processDefinition, ProcessDefinitionActionRequest actionRequest);

    void accessProcessDefinitionIdentityLinks(ProcessDefinition processDefinition);

    void accessProcessDefinitionIdentityLink(ProcessDefinition processDefinition, IdentityLink identityLink);

    void deleteProcessDefinitionIdentityLink(ProcessDefinition processDefinition, IdentityLink identityLink);

    void createProcessDefinitionIdentityLink(ProcessDefinition processDefinition, RestIdentityLink identityLink);

    void accessProcessDefinitionsWithQuery(ProcessDefinitionQuery processDefinitionQuery);
    
    void accessDeploymentById(Deployment deployment);
    
    void accessDeploymentsWithQuery(DeploymentQuery deploymentQuery);
    
    void executeNewDeploymentForTenantId(String tenantId);

    void enhanceDeployment(DeploymentBuilder deploymentBuilder);
    
    void deleteDeployment(Deployment deployment);
    
    void accessModelInfoById(Model model);
    
    void accessModelInfoWithQuery(ModelQuery modelQuery);
    
    void createModel(Model model, ModelRequest request);
    
    void accessJobInfoById(Job job);

    void accessHistoryJobInfoById(HistoryJob job);
    
    void accessJobInfoWithQuery(JobQuery jobQuery);
    
    void accessTimerJobInfoWithQuery(TimerJobQuery jobQuery);
    
    void accessSuspendedJobInfoWithQuery(SuspendedJobQuery jobQuery);
    
    void accessDeadLetterJobInfoWithQuery(DeadLetterJobQuery jobQuery);

    void accessHistoryJobInfoWithQuery(HistoryJobQuery jobQuery);
    
    void deleteJob(Job job);

    void deleteHistoryJob(HistoryJob historyJob);
    
    void accessBatchInfoById(Batch batch);
    
    void accessBatchInfoWithQuery(BatchQuery batchQuery);
    
    void deleteBatch(Batch batch);
    
    void accessBatchPartInfoOfBatch(Batch batch);
    
    void accessBatchPartInfoById(BatchPart batchPart);
    
    void accessManagementInfo();
    
    void accessTableInfo();
    
    void accessHistoryTaskInfoById(HistoricTaskInstance historicTaskInstance);
    
    void accessHistoryTaskInfoWithQuery(HistoricTaskInstanceQuery historicTaskInstanceQuery, HistoricTaskInstanceQueryRequest request);
    
    void deleteHistoricTask(HistoricTaskInstance historicTaskInstance);
    
    void accessHistoricTaskIdentityLinks(HistoricTaskInstance historicTaskInstance);

    void accessHistoryProcessInfoById(HistoricProcessInstance historicProcessInstance);
    
    void accessHistoryProcessInfoWithQuery(HistoricProcessInstanceQuery historicProcessInstanceQuery, HistoricProcessInstanceQueryRequest request);
    
    void deleteHistoricProcess(HistoricProcessInstance historicProcessInstance);
    
    void accessHistoricProcessIdentityLinks(HistoricProcessInstance historicProcessInstance);

    void accessHistoryActivityInfoWithQuery(HistoricActivityInstanceQuery historicActivityInstanceQuery, HistoricActivityInstanceQueryRequest request);
    
    void accessHistoryDetailById(HistoricDetail historicDetail);
    
    void accessHistoryDetailInfoWithQuery(HistoricDetailQuery historicDetailQuery, HistoricDetailQueryRequest request);
    
    void accessHistoryVariableInfoById(HistoricVariableInstance historicVariableInstance);
    
    void accessHistoryVariableInfoWithQuery(HistoricVariableInstanceQuery historicVariableInstanceQuery, HistoricVariableInstanceQueryRequest request);

    void accessHistoricTaskLogWithQuery(HistoricTaskLogEntryQuery historicTaskLogEntryQuery, HistoricTaskLogEntryQueryRequest request);

    void accessGroupInfoById(Group group);
    
    void accessGroupInfoWithQuery(GroupQuery groupQuery);
    
    void createGroup(GroupRequest groupRequest);
    
    void deleteGroup(Group group);
    
    void accessUserInfoById(User user);
    
    void accessUserInfoWithQuery(UserQuery userQuery);
    
    void createUser(UserRequest userRequest);
    
    void deleteUser(User user);
    
    void accessFormData(FormData formData);
    
    void submitFormData(SubmitFormRequest formRequest);
}
