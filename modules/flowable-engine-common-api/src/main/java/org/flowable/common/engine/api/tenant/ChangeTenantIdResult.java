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

 package org.flowable.common.engine.api.tenant;

 import java.util.Map;

/**
 * Container interface to return the result of a change in a tenant id operation
 */
public interface ChangeTenantIdResult {

    public static final String ACTIVITY_INSTANCES = "ActivityInstances";
    public static final String EXECUTIONS = "Executions";
    public static final String CASE_INSTANCES = "CaseInstances";
    public static final String PLAN_ITEM_INSTANCES = "PlanItemInstances";
    public static final String MILESTONE_INSTANCES = "MilestoneInstances";
    public static final String EVENT_SUBSCRIPTIONS = "EventSubscriptions";
    public static final String TASKS = "Tasks";
    public static final String FORM_INSTANCES = "FormInstances";
    public static final String EXTERNAL_WORKER_JOBS = "ExternalWorkerJobs";
    public static final String CONTENT_ITEM_INSTANCES = "ContentItemInstances";
    public static final String HISTORIC_ACTIVITY_INSTANCES = "HistoricActivityInstances";
    public static final String HISTORIC_CASE_INSTANCES = "HistoricCaseInstances";
    public static final String HISTORIC_DECISION_EXECUTIONS = "HistoricDecisionExecutions";
    public static final String HISTORIC_MILESTONE_INSTANCES = "HistoricMilestoneInstances";
    public static final String HISTORIC_PLAN_ITEM_INSTANCES = "HistoricPlanItemInstances";
    public static final String HISTORIC_PROCESS_INSTANCES = "HistoricProcessInstances";
    public static final String HISTORIC_TASK_LOG_ENTRIES = "HistoricTaskLogEntries";
    public static final String HISTORIC_TASK_INSTANCES = "HistoricTaskInstances";
    public static final String HISTORY_JOBS = "HistoryJobs";
    public static final String JOBS = "Jobs";
    public static final String SUSPENDED_JOBS = "SuspendedJobs";
    public static final String TIMER_JOBS = "TimerJobs";
    public static final String DEADLETTER_JOBS = "DeadLetterJobs";

    /**
     * Gets the result of the changed instances of a certain entity type.
     */
    long getChangedInstances(String entityType);

    /**
     * Puts the amount of changed instances for a certain entityType. Follows the {@link java.util.Map#put(Object, Object) put} logic.
     */
    long putResult(String entityType, Long amountOfChangedInstances);

    /**
     * Puts a map of results with the amounts of changed instances for a group of entityTypes. Follows the {@link java.util.Map#putAll(Map) putAll} logic.
     * @param resultMap
     */
    void putAllResults(Map<String, Long> resultMap);

}