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
package org.flowable.cmmn.rest.service.api;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.history.HistoricMilestoneInstanceQuery;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstanceQuery;
import org.flowable.cmmn.api.history.HistoricVariableInstanceQuery;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.repository.CmmnDeploymentBuilder;
import org.flowable.cmmn.api.repository.CmmnDeploymentQuery;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.flowable.cmmn.rest.service.api.history.caze.HistoricCaseInstanceQueryRequest;
import org.flowable.cmmn.rest.service.api.history.milestone.HistoricMilestoneInstanceQueryRequest;
import org.flowable.cmmn.rest.service.api.history.planitem.HistoricPlanItemInstanceQueryRequest;
import org.flowable.cmmn.rest.service.api.history.task.HistoricTaskInstanceQueryRequest;
import org.flowable.cmmn.rest.service.api.history.variable.HistoricVariableInstanceQueryRequest;
import org.flowable.cmmn.rest.service.api.runtime.caze.CaseInstanceCreateRequest;
import org.flowable.cmmn.rest.service.api.runtime.caze.CaseInstanceQueryRequest;
import org.flowable.cmmn.rest.service.api.runtime.caze.CaseInstanceUpdateRequest;
import org.flowable.cmmn.rest.service.api.runtime.caze.ChangePlanItemStateRequest;
import org.flowable.cmmn.rest.service.api.runtime.planitem.PlanItemInstanceQueryRequest;
import org.flowable.cmmn.rest.service.api.runtime.task.TaskActionRequest;
import org.flowable.cmmn.rest.service.api.runtime.task.TaskQueryRequest;
import org.flowable.cmmn.rest.service.api.runtime.task.TaskRequest;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionQuery;
import org.flowable.job.api.DeadLetterJobQuery;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.HistoryJobQuery;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobQuery;
import org.flowable.job.api.SuspendedJobQuery;
import org.flowable.job.api.TimerJobQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;

public interface CmmnRestApiInterceptor {

    void accessTaskInfoById(Task task);
    
    void accessTaskInfoWithQuery(TaskQuery taskQuery, TaskQueryRequest request);
    
    void createTask(Task task, TaskRequest request);
    
    void updateTask(Task task, TaskRequest request);

    void deleteTask(Task task);
    
    void executeTaskAction(Task task, TaskActionRequest actionRequest);
    
    void accessCaseInstanceInfoById(CaseInstance caseInstance);

    void accessCaseInstanceInfoWithQuery(CaseInstanceQuery caseInstanceQuery, CaseInstanceQueryRequest request);
    
    void createCaseInstance(CaseInstanceBuilder caseInstanceBuilder, CaseInstanceCreateRequest request);
    
    void terminateCaseInstance(CaseInstance caseInstance);
    
    void deleteCaseInstance(CaseInstance caseInstance);
    
    void doCaseInstanceAction(CaseInstance caseInstance, RestActionRequest actionRequest);

    void updateCaseInstance(CaseInstance caseInstance, CaseInstanceUpdateRequest updateRequest);
    
    void accessPlanItemInstanceInfoById(PlanItemInstance planItemInstance);

    void accessPlanItemInstanceInfoWithQuery(PlanItemInstanceQuery planItemInstanceQuery, PlanItemInstanceQueryRequest request);
    
    void doPlanItemInstanceAction(PlanItemInstance planItemInstance, RestActionRequest actionRequest);
    
    void accessCaseDefinitionById(CaseDefinition caseDefinition);
    
    void accessCaseDefinitionsWithQuery(CaseDefinitionQuery caseDefinitionQuery);
    
    void accessDeploymentById(CmmnDeployment deployment);
    
    void accessDeploymentsWithQuery(CmmnDeploymentQuery deploymentQuery);
    
    void executeNewDeploymentForTenantId(String tenantId);

    void enhanceDeployment(CmmnDeploymentBuilder cmmnDeploymentBuilder);
    
    void deleteDeployment(CmmnDeployment deployment);
    
    void accessJobInfoById(Job job);
    
    void accessJobInfoWithQuery(JobQuery jobQuery);
    
    void accessTimerJobInfoWithQuery(TimerJobQuery jobQuery);

    void accessHistoryJobInfoWithQuery(HistoryJobQuery jobQuery);
    
    void accessSuspendedJobInfoWithQuery(SuspendedJobQuery jobQuery);
    
    void accessDeadLetterJobInfoWithQuery(DeadLetterJobQuery jobQuery);

    void accessHistoryJobInfoById(HistoryJob historyJob);
    
    void deleteJob(Job job);

    void deleteHistoryJob(HistoryJob historyJob);
    
    void accessEventSubscriptionById(EventSubscription eventSubscription);
    
    void accessEventSubscriptionInfoWithQuery(EventSubscriptionQuery eventSubscriptionQuery);
    
    void accessManagementInfo();
    
    void accessTableInfo();
    
    void accessHistoryTaskInfoById(HistoricTaskInstance historicTaskInstance);
    
    void accessHistoryTaskInfoWithQuery(HistoricTaskInstanceQuery historicTaskInstanceQuery, HistoricTaskInstanceQueryRequest request);
    
    void deleteHistoricTask(HistoricTaskInstance historicTaskInstance);
    
    void accessHistoryCaseInfoById(HistoricCaseInstance historicCaseInstance);
    
    void accessHistoryCaseInfoWithQuery(HistoricCaseInstanceQuery historicCaseInstanceQuery, HistoricCaseInstanceQueryRequest request);
    
    void deleteHistoricCase(HistoricCaseInstance historicCaseInstance);
    
    void accessStageOverview(CaseInstance caseInstance);

    void accessHistoryMilestoneInfoById(HistoricMilestoneInstance historicMilestoneInstance);
    
    void accessHistoryMilestoneInfoWithQuery(HistoricMilestoneInstanceQuery historicMilestoneInstanceQuery, HistoricMilestoneInstanceQueryRequest request);
    
    void accessHistoryPlanItemInfoById(HistoricPlanItemInstance historicPlanItemInstance);
    
    void accessHistoryPlanItemInfoWithQuery(HistoricPlanItemInstanceQuery historicPlanItemInstanceQuery, HistoricPlanItemInstanceQueryRequest request);
    
    void accessHistoryVariableInfoById(HistoricVariableInstance historicVariableInstance);
    
    void accessHistoryVariableInfoWithQuery(HistoricVariableInstanceQuery historicVariableInstanceQuery, HistoricVariableInstanceQueryRequest request);

    void migrateCaseInstance(String caseInstanceId, String migrationDocumentJson);
    
    void changePlanItemState(String caseInstanceId, ChangePlanItemStateRequest planItemStateRequest);
}
