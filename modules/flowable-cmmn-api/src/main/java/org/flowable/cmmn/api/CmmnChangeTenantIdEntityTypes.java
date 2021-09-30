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
package org.flowable.cmmn.api;

/**
 * @author Filip Hrisafov
 */
public interface CmmnChangeTenantIdEntityTypes {

    String CASE_INSTANCES = "CaseInstances";
    String PLAN_ITEM_INSTANCES = "PlanItemInstances";
    String MILESTONE_INSTANCES = "MilestoneInstances";
    String EVENT_SUBSCRIPTIONS = "CmmnEventSubscriptions";
    String TASKS = "CmmnTasks";
    String EXTERNAL_WORKER_JOBS = "CmmnExternalWorkerJobs";
    String HISTORIC_CASE_INSTANCES = "HistoricCaseInstances";
    String HISTORIC_MILESTONE_INSTANCES = "HistoricMilestoneInstances";
    String HISTORIC_PLAN_ITEM_INSTANCES = "HistoricPlanItemInstances";
    String HISTORIC_TASK_LOG_ENTRIES = "CmmnHistoricTaskLogEntries";
    String HISTORIC_TASK_INSTANCES = "CmmnHistoricTaskInstances";
    String JOBS = "CmmnJobs";
    String SUSPENDED_JOBS = "CmmnSuspendedJobs";
    String TIMER_JOBS = "CmmnTimerJobs";
    String DEADLETTER_JOBS = "CmmnDeadLetterJobs";

}
