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

import org.flowable.engine.form.FormData;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.history.HistoricDetailQuery;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentQuery;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ModelQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.EventSubscription;
import org.flowable.engine.runtime.EventSubscriptionQuery;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.GroupQuery;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.flowable.job.api.DeadLetterJobQuery;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobQuery;
import org.flowable.job.api.SuspendedJobQuery;
import org.flowable.job.api.TimerJobQuery;
import org.flowable.rest.service.api.form.SubmitFormRequest;
import org.flowable.rest.service.api.history.HistoricActivityInstanceQueryRequest;
import org.flowable.rest.service.api.history.HistoricDetailQueryRequest;
import org.flowable.rest.service.api.history.HistoricProcessInstanceQueryRequest;
import org.flowable.rest.service.api.history.HistoricTaskInstanceQueryRequest;
import org.flowable.rest.service.api.history.HistoricVariableInstanceQueryRequest;
import org.flowable.rest.service.api.identity.GroupRequest;
import org.flowable.rest.service.api.identity.UserRequest;
import org.flowable.rest.service.api.runtime.process.ExecutionActionRequest;
import org.flowable.rest.service.api.runtime.process.ExecutionChangeActivityStateRequest;
import org.flowable.rest.service.api.runtime.process.ExecutionQueryRequest;
import org.flowable.rest.service.api.runtime.process.InjectActivityRequest;
import org.flowable.rest.service.api.runtime.process.ProcessInstanceQueryRequest;
import org.flowable.rest.service.api.runtime.process.SignalEventReceivedRequest;
import org.flowable.rest.service.api.runtime.task.TaskActionRequest;
import org.flowable.rest.service.api.runtime.task.TaskQueryRequest;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;

public interface BpmnRestApiInterceptor {

    void accessTaskInfoById(Task task);
    
    void accessTaskInfoWithQuery(TaskQuery taskQuery, TaskQueryRequest request);
    
    void createTask(Task task);
    
    void deleteTask(Task task);
    
    void executeTaskAction(Task task, TaskActionRequest actionRequest);
    
    void accessExecutionInfoById(Execution execution);

    void accessExecutionInfoWithQuery(ExecutionQuery executionQuery, ExecutionQueryRequest request);
    
    void doExecutionActionRequest(ExecutionActionRequest executionActionRequest);
    
    void accessProcessInstanceInfoById(ProcessInstance processInstance);

    void accessProcessInstanceInfoWithQuery(ProcessInstanceQuery processInstanceQuery, ProcessInstanceQueryRequest request);
    
    void createProcessInstance(ProcessInstanceBuilder processInstanceBuilder);
    
    void deleteProcessInstance(ProcessInstance processInstance);
    
    void sendSignal(SignalEventReceivedRequest signalEventReceivedRequest);
    
    void changeActivityState(ExecutionChangeActivityStateRequest changeActivityStateRequest);
    
    void injectActivity(InjectActivityRequest injectActivityRequest);
    
    void accessEventSubscriptionById(EventSubscription eventSubscription);
    
    void accessEventSubscriptionInfoWithQuery(EventSubscriptionQuery eventSubscriptionQuery);
    
    void accessProcessDefinitionById(ProcessDefinition processDefinition);
    
    void accessProcessDefinitionsWithQuery(ProcessDefinitionQuery processDefinitionQuery);
    
    void accessDeploymentById(Deployment deployment);
    
    void accessDeploymentsWithQuery(DeploymentQuery deploymentQuery);
    
    void executeNewDeploymentForTenantId(String tenantId);
    
    void deleteDeployment(Deployment deployment);
    
    void accessModelInfoById(Model model);
    
    void accessModelInfoWithQuery(ModelQuery modelQuery);
    
    void createModel(Model model);
    
    void accessJobInfoById(Job job);
    
    void accessJobInfoWithQuery(JobQuery jobQuery);
    
    void accessTimerJobInfoWithQuery(TimerJobQuery jobQuery);
    
    void accessSuspendedJobInfoWithQuery(SuspendedJobQuery jobQuery);
    
    void accessDeadLetterJobInfoWithQuery(DeadLetterJobQuery jobQuery);
    
    void deleteJob(Job job);
    
    void accessManagementInfo();
    
    void accessTableInfo();
    
    void accessHistoryTaskInfoById(HistoricTaskInstance historicTaskInstance);
    
    void accessHistoryTaskInfoWithQuery(HistoricTaskInstanceQuery historicTaskInstanceQuery, HistoricTaskInstanceQueryRequest request);
    
    void deleteHistoricTask(HistoricTaskInstance historicTaskInstance);
    
    void accessHistoryProcessInfoById(HistoricProcessInstance historicProcessInstance);
    
    void accessHistoryProcessInfoWithQuery(HistoricProcessInstanceQuery historicProcessInstanceQuery, HistoricProcessInstanceQueryRequest request);
    
    void deleteHistoricProcess(HistoricProcessInstance historicProcessInstance);
    
    void accessHistoryActivityInfoWithQuery(HistoricActivityInstanceQuery historicActivityInstanceQuery, HistoricActivityInstanceQueryRequest request);
    
    void accessHistoryDetailById(HistoricDetail historicDetail);
    
    void accessHistoryDetailInfoWithQuery(HistoricDetailQuery historicDetailQuery, HistoricDetailQueryRequest request);
    
    void accessHistoryVariableInfoById(HistoricVariableInstance historicVariableInstance);
    
    void accessHistoryVariableInfoWithQuery(HistoricVariableInstanceQuery historicVariableInstanceQuery, HistoricVariableInstanceQueryRequest request);
    
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
