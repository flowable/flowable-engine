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
package org.flowable.engine.test.api.runtime.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.assertj.core.groups.Tuple;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Bas Claessen
 */
public class ProcessInstanceMigrationEventRegistryEventSubprocessTest extends AbstractProcessInstanceMigrationEventRegistryConsumerTest {

    @Test
    public void testMigrateNonInterruptingEventRegistryEventSubProcess() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/non-interrupting-eventregistry-event-subprocess.bpmn20.xml");

        //Start an instance of the first version of the process for migration
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(version1ProcessDef.getKey());

        //Deploy second version of the same process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/non-interrupting-eventregistry-event-subprocess.bpmn20.xml");
        assertThat(version1ProcessDef.getId()).isNotEqualTo(version2ProcessDef.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("processTask", "eventSubProcessStart");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(version1ProcessDef.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("processTask", version1ProcessDef.getId()));
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getEventType, EventSubscription::getActivityId, EventSubscription::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("myEvent", "eventSubProcessStart", version1ProcessDef.getId()));

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId());

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("processTask", "eventSubProcessStart");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(version2ProcessDef.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("processTask", version2ProcessDef.getId()));

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getEventType, EventSubscription::getActivityId, EventSubscription::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("myEvent", "eventSubProcessStart", version2ProcessDef.getId()));

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateNonInterruptingEventRegistryEventSubProcessWithStartedSubProcess() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/non-interrupting-eventregistry-event-subprocess.bpmn20.xml");

        //Start an instance of the first version of the process for migration
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(version1ProcessDef.getKey());

        //Deploy second version of the same process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/non-interrupting-eventregistry-event-subprocess.bpmn20.xml");
        assertThat(version1ProcessDef.getId()).isNotEqualTo(version2ProcessDef.getId());

        //Trigger event
        inboundEventChannelAdapter.triggerTestEvent();

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("processTask", "eventSubProcessStart", "eventSubProcess", "eventSubProcessTask");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(version1ProcessDef.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("processTask", version1ProcessDef.getId()),
                        Tuple.tuple("eventSubProcessTask", version1ProcessDef.getId())
                );
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getEventType, EventSubscription::getActivityId, EventSubscription::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("myEvent", "eventSubProcessStart", version1ProcessDef.getId()));

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId());

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("processTask", "eventSubProcessStart", "eventSubProcess", "eventSubProcessTask");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(version2ProcessDef.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("processTask", version2ProcessDef.getId()),
                        Tuple.tuple("eventSubProcessTask", version2ProcessDef.getId())
                );

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getEventType, EventSubscription::getActivityId, EventSubscription::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("myEvent", "eventSubProcessStart", version2ProcessDef.getId()));

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateNonInterruptingEventRegistryEventSubProcessWithTwoStartedSubProcess() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/non-interrupting-eventregistry-event-subprocess.bpmn20.xml");

        //Start an instance of the first version of the process for migration
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(version1ProcessDef.getKey());

        //Deploy second version of the same process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/non-interrupting-eventregistry-event-subprocess.bpmn20.xml");
        assertThat(version1ProcessDef.getId()).isNotEqualTo(version2ProcessDef.getId());

        //Trigger event to create first sub process
        inboundEventChannelAdapter.triggerTestEvent();
        //Trigger event to create second sub process
        inboundEventChannelAdapter.triggerTestEvent();

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder(
                        "processTask", "eventSubProcessStart", "eventSubProcess", "eventSubProcessTask", "eventSubProcess", "eventSubProcessTask"
                );
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(version1ProcessDef.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("processTask", version1ProcessDef.getId()),
                        Tuple.tuple("eventSubProcessTask", version1ProcessDef.getId()),
                        Tuple.tuple("eventSubProcessTask", version1ProcessDef.getId())
                );
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getEventType, EventSubscription::getActivityId, EventSubscription::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("myEvent", "eventSubProcessStart", version1ProcessDef.getId()));

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId());

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder(
                        "processTask", "eventSubProcessStart", "eventSubProcess", "eventSubProcessTask", "eventSubProcess", "eventSubProcessTask"
                );
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(version2ProcessDef.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("processTask", version2ProcessDef.getId()),
                        Tuple.tuple("eventSubProcessTask", version2ProcessDef.getId()),
                        Tuple.tuple("eventSubProcessTask", version2ProcessDef.getId())
                );

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getEventType, EventSubscription::getActivityId, EventSubscription::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("myEvent", "eventSubProcessStart", version2ProcessDef.getId()));

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    private EventSubscriptionQueryImpl createEventSubscriptionQuery() {
        return new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutor(), processEngineConfiguration.getEventSubscriptionServiceConfiguration());
    }
}
