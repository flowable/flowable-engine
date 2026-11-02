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
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the variable aggregation overview variable keeps working after a parent process instance with
 * an active, in-flight multi-instance call activity (sequential and parallel) is migrated to a new version.
 *
 * The overview variable of type {@code bpmnAggregation} stores the id of the multi-instance root execution
 * (see {@code MultiInstanceActivityBehavior#execute}). Because the active execution is inside a multi-instance
 * container, the migration cannot use the direct path and recreates the multi-instance executions, giving the
 * multi-instance root a new id. Previously the overview variable kept referencing the old (now removed)
 * execution id, so reading it did {@code findById(oldId)} -> {@code null} ->
 * {@code NullPointerException} on {@code parentExecution.isMultiInstanceRoot()}
 * ({@code BpmnAggregation.aggregateOverview}). The migration now updates those references to the new
 * multi-instance root execution id, so the overview variable is readable again after the migration.
 *
 * The BPMN is an open source extraction of the Design app 'validationDOAPRBpmn' (parent) and 'getAvisProcess'
 * (child), keeping only the process logic (forms/DMN removed).
 */
public class ProcessInstanceMigrationVariableAggregationTest extends AbstractProcessInstanceMigrationTest {

    protected static final String CHILD = "org/flowable/engine/test/api/runtime/migration/aggregation-child.bpmn20.xml";
    protected static final String PARENT_V1 = "org/flowable/engine/test/api/runtime/migration/aggregation-parent-v1.bpmn20.xml";
    protected static final String PARENT_V2 = "org/flowable/engine/test/api/runtime/migration/aggregation-parent-v2.bpmn20.xml";
    protected static final String PARENT_PARALLEL_V1 = "org/flowable/engine/test/api/runtime/migration/aggregation-parent-parallel-v1.bpmn20.xml";
    protected static final String PARENT_PARALLEL_V2 = "org/flowable/engine/test/api/runtime/migration/aggregation-parent-parallel-v2.bpmn20.xml";

    @AfterEach
    protected void tearDown() {
        deleteDeployments();
    }

    @Test
    public void testReadOverviewVariableAfterMigrationOfParentWithActiveSequentialMultiInstanceCallActivity() {
        assertOverviewVariableSurvivesMigration("validationParent", PARENT_V1, PARENT_V2);
    }

    @Test
    public void testReadOverviewVariableAfterMigrationOfParentWithActiveParallelMultiInstanceCallActivity() {
        assertOverviewVariableSurvivesMigration("validationParentParallel", PARENT_PARALLEL_V1, PARENT_PARALLEL_V2);
    }

    protected void assertOverviewVariableSurvivesMigration(String processKey, String parentV1Resource, String parentV2Resource) {
        // Child process, deployed once (resolved by key by the call activity)
        deployProcessDefinition("child deploy", CHILD);

        // Parent version 1
        deployProcessDefinition("parent deploy", parentV1Resource);

        // Start the parent with a collection of two 'decideurs'. The call activity is a multi-instance that
        // starts an 'avisChild' instance per element, whose 'Sub task' user task is now waiting (one iteration
        // for the sequential variant, both iterations for the parallel variant). The multi-instance is
        // therefore in-flight (not yet completed).
        ProcessInstance parentInstance = runtimeService.startProcessInstanceByKey(processKey,
                Map.of("decideurs", Arrays.asList("respEU", "DGA")));

        // Sanity: at least one child 'Sub task' is waiting and the multi-instance is active
        assertThat(taskService.createTaskQuery().taskName("Sub task").list()).isNotEmpty();

        // The multi-instance root execution exists before migration. Its id is what the overview aggregation
        // variable references internally.
        Set<String> miRootIdsBeforeMigration = multiInstanceRootExecutionIds(parentInstance.getId());

        // Before migration, reading the overview aggregation variable works (the stored execution id still
        // resolves to the live multi-instance root execution).
        assertThatCode(() -> runtimeService.getVariable(parentInstance.getId(), "overviewAvis"))
                .as("overview variable is readable before migration")
                .doesNotThrowAnyException();

        // Parent version 2 (same activity ids -> auto mapping)
        ProcessDefinition parentV2 = deployProcessDefinition("parent deploy v2", parentV2Resource);

        // Migrate the parent process instance to v2.
        processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(parentV2.getId())
                .migrate(parentInstance.getId());

        // The multi-instance root execution has been recreated with a different id during migration. This is
        // the underlying cause: the overview variable used to keep referencing the old (now removed) id.
        Set<String> miRootIdsAfterMigration = multiInstanceRootExecutionIds(parentInstance.getId());
        if (!miRootIdsBeforeMigration.isEmpty() && !miRootIdsAfterMigration.isEmpty()) {
            assertThat(miRootIdsAfterMigration)
                    .as("multi-instance root execution id changes during migration")
                    .doesNotContainAnyElementsOf(miRootIdsBeforeMigration);
        }

        // Before the fix, the overview aggregation variable still referenced the OLD (now removed) multi-instance
        // root execution id, so reading it triggered BpmnAggregation.aggregateOverview -> findById(oldId) -> null
        // -> NullPointerException on parentExecution.isMultiInstanceRoot(). The migration now updates the
        // reference to the new multi-instance root execution, so reading it returns cleanly and still resolves
        // to an actual overview value.
        assertThatCode(() -> runtimeService.getVariable(parentInstance.getId(), "overviewAvis"))
                .as("reading the overview variable after migration must not throw")
                .doesNotThrowAnyException();
        assertThat(runtimeService.getVariable(parentInstance.getId(), "overviewAvis"))
                .as("overview variable resolves to a value again after migration (its execution reference was updated)")
                .isNotNull();

        // Reading the full variable map (what the platform does when fetching task variables for the sub
        // process user task) also returns cleanly.
        assertThatCode(() -> runtimeService.getVariables(parentInstance.getId()))
                .as("reading all variables after migration must not throw")
                .doesNotThrowAnyException();
        assertThat(runtimeService.getVariables(parentInstance.getId())).containsKey("overviewAvis");

        // The migrated instance stays healthy: it can be driven to completion normally.
        List<Task> subTasks;
        while (!(subTasks = taskService.createTaskQuery().taskName("Sub task").list()).isEmpty()) {
            taskService.complete(subTasks.get(0).getId(), Map.of("decision", "approved"));
        }
        Task afterTask = taskService.createTaskQuery().taskName("After task").singleResult();
        assertThat(afterTask).isNotNull();
        taskService.complete(afterTask.getId());
        assertProcessEnded(parentInstance.getId());
    }

    protected Set<String> multiInstanceRootExecutionIds(String processInstanceId) {
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).list();
        return executions.stream()
                .map(e -> (ExecutionEntity) e)
                .filter(ExecutionEntity::isMultiInstanceRoot)
                .map(ExecutionEntity::getId)
                .collect(Collectors.toSet());
    }
}
