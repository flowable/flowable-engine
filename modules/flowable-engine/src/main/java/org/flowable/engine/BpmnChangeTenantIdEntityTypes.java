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
package org.flowable.engine;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public interface BpmnChangeTenantIdEntityTypes {

    String ACTIVITY_INSTANCES = "ActivityInstances";
    String EXECUTIONS = "Executions";
    String EVENT_SUBSCRIPTIONS = "BpmnEventSubscriptions";
    String TASKS = "BpmnTasks";
    String EXTERNAL_WORKER_JOBS = "BpmnExternalWorkerJobs";
    String HISTORIC_ACTIVITY_INSTANCES = "HistoricActivityInstances";
    String HISTORIC_PROCESS_INSTANCES = "HistoricProcessInstances";
    String HISTORIC_TASK_LOG_ENTRIES = "BpmnHistoricTaskLogEntries";
    String HISTORIC_TASK_INSTANCES = "BpmnHistoricTaskInstances";
    String JOBS = "BpmnJobs";
    String SUSPENDED_JOBS = "BpmnSuspendedJobs";
    String TIMER_JOBS = "BpmnTimerJobs";
    String DEADLETTER_JOBS = "BpmnDeadLetterJobs";

    Set<String> RUNTIME_TYPES = new LinkedHashSet<>(Arrays.asList(
            BpmnChangeTenantIdEntityTypes.EXECUTIONS,
            BpmnChangeTenantIdEntityTypes.ACTIVITY_INSTANCES,
            BpmnChangeTenantIdEntityTypes.EVENT_SUBSCRIPTIONS,
            BpmnChangeTenantIdEntityTypes.DEADLETTER_JOBS,
            BpmnChangeTenantIdEntityTypes.EXTERNAL_WORKER_JOBS,
            BpmnChangeTenantIdEntityTypes.JOBS,
            BpmnChangeTenantIdEntityTypes.SUSPENDED_JOBS,
            BpmnChangeTenantIdEntityTypes.TIMER_JOBS,
            BpmnChangeTenantIdEntityTypes.TASKS
    ));

    Set<String> HISTORIC_TYPES = new LinkedHashSet<>(Arrays.asList(
            BpmnChangeTenantIdEntityTypes.HISTORIC_PROCESS_INSTANCES,
            BpmnChangeTenantIdEntityTypes.HISTORIC_ACTIVITY_INSTANCES,
            BpmnChangeTenantIdEntityTypes.HISTORIC_TASK_INSTANCES,
            BpmnChangeTenantIdEntityTypes.HISTORIC_TASK_LOG_ENTRIES
    ));

}
