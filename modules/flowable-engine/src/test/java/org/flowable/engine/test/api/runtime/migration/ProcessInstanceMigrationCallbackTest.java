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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.migration.ProcessInstanceMigrationCallback;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link ProcessInstanceMigrationCallback} receives the source process definition the instance was migrated
 * from, and that a callback implementing only the deprecated method is still invoked (backwards compatibility).
 */
public class ProcessInstanceMigrationCallbackTest extends AbstractProcessInstanceMigrationTest {

    protected static final String ONE_TASK_PROCESS = "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml";

    @AfterEach
    void resetMigrationCallbacks() {
        processEngineConfiguration.setProcessInstanceMigrationCallbacks(null);
        deleteDeployments();
    }

    @Test
    public void migrationProvidesSourceProcessDefinition() {
        RecordingCallback callback = new RecordingCallback();
        processEngineConfiguration.setProcessInstanceMigrationCallbacks(Collections.singletonList(callback));

        ProcessDefinition sourceDefinition = deployProcessDefinition("my deploy", ONE_TASK_PROCESS);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");
        ProcessDefinition destinationDefinition = deployProcessDefinition("my deploy", ONE_TASK_PROCESS);

        processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(destinationDefinition.getId())
                .migrate(processInstance.getId());

        assertThat(callback.getMigrations()).hasSize(1);
        RecordedMigration migration = callback.getMigrations().get(0);
        assertThat(migration.getSource().getId()).isEqualTo(sourceDefinition.getId());
        assertThat(migration.getSource().getVersion()).isEqualTo(1);
        assertThat(migration.getTarget().getId()).isEqualTo(destinationDefinition.getId());
        assertThat(migration.getTarget().getVersion()).isEqualTo(2);
    }

    @Test
    public void legacyCallbackStillInvoked() {
        LegacyRecordingCallback callback = new LegacyRecordingCallback();
        processEngineConfiguration.setProcessInstanceMigrationCallbacks(Collections.singletonList(callback));

        deployProcessDefinition("my deploy", ONE_TASK_PROCESS);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");
        ProcessDefinition destinationDefinition = deployProcessDefinition("my deploy", ONE_TASK_PROCESS);

        processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(destinationDefinition.getId())
                .migrate(processInstance.getId());

        assertThat(callback.getTargetDefinitionIds()).containsExactly(destinationDefinition.getId());
    }

    static class RecordedMigration {

        private final ProcessDefinition source;
        private final ProcessDefinition target;

        RecordedMigration(ProcessDefinition source, ProcessDefinition target) {
            this.source = source;
            this.target = target;
        }

        public ProcessDefinition getSource() {
            return source;
        }

        public ProcessDefinition getTarget() {
            return target;
        }
    }

    static class RecordingCallback implements ProcessInstanceMigrationCallback {

        private final List<RecordedMigration> migrations = new ArrayList<>();

        @Override
        public void processInstanceMigrated(ProcessInstance processInstance, ProcessDefinition procDefToMigrateTo,
                ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        }

        @Override
        public void processInstanceMigrated(ProcessInstance processInstance, ProcessDefinition sourceProcessDefinition,
                ProcessDefinition procDefToMigrateTo, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
            migrations.add(new RecordedMigration(sourceProcessDefinition, procDefToMigrateTo));
        }

        public List<RecordedMigration> getMigrations() {
            return migrations;
        }
    }

    /** Implements only the deprecated method to verify the new overload delegates to it. */
    static class LegacyRecordingCallback implements ProcessInstanceMigrationCallback {

        private final List<String> targetDefinitionIds = new ArrayList<>();

        @Override
        public void processInstanceMigrated(ProcessInstance processInstance, ProcessDefinition procDefToMigrateTo,
                ProcessInstanceMigrationDocument document, CommandContext commandContext) {
            targetDefinitionIds.add(procDefToMigrateTo.getId());
        }

        public List<String> getTargetDefinitionIds() {
            return targetDefinitionIds;
        }
    }

}
