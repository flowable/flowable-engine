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

package org.flowable.cmmn.test.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationCallback;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.HistoricCaseInstanceMigrationDocument;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link CaseInstanceMigrationCallback} receives the source case definition the instance was migrated from,
 * and that callbacks implementing only the deprecated methods are still invoked (backwards compatibility).
 */
public class CaseInstanceMigrationCallbackTest extends AbstractCaseMigrationTest {

    protected static final String ONE_TASK_CASE = "org/flowable/cmmn/test/migration/one-task.cmmn.xml";

    @AfterEach
    void resetMigrationCallbacks() {
        cmmnEngineConfiguration.setCaseInstanceMigrationCallbacks(null);
    }

    @Test
    void runtimeMigrationProvidesSourceCaseDefinition() {
        RecordingCallback callback = new RecordingCallback();
        cmmnEngineConfiguration.setCaseInstanceMigrationCallbacks(Collections.singletonList(callback));

        CaseDefinition sourceDefinition = deployCaseDefinition("test1", ONE_TASK_CASE);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", ONE_TASK_CASE);

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .migrate(caseInstance.getId());

        assertThat(callback.getRuntimeMigrations()).hasSize(1);
        RecordedMigration migration = callback.getRuntimeMigrations().get(0);
        assertThat(migration.getSource().getId()).isEqualTo(sourceDefinition.getId());
        assertThat(migration.getSource().getVersion()).isEqualTo(1);
        assertThat(migration.getTarget().getId()).isEqualTo(destinationDefinition.getId());
        assertThat(migration.getTarget().getVersion()).isEqualTo(2);
    }

    @Test
    void historicMigrationProvidesSourceCaseDefinition() {
        RecordingCallback callback = new RecordingCallback();
        cmmnEngineConfiguration.setCaseInstanceMigrationCallbacks(Collections.singletonList(callback));

        CaseDefinition sourceDefinition = deployCaseDefinition("test1", ONE_TASK_CASE);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", ONE_TASK_CASE);

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        cmmnMigrationService.createHistoricCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .migrate(caseInstance.getId());

        assertThat(callback.getHistoricMigrations()).hasSize(1);
        RecordedMigration migration = callback.getHistoricMigrations().get(0);
        assertThat(migration.getSource().getId()).isEqualTo(sourceDefinition.getId());
        assertThat(migration.getSource().getVersion()).isEqualTo(1);
        assertThat(migration.getTarget().getId()).isEqualTo(destinationDefinition.getId());
        assertThat(migration.getTarget().getVersion()).isEqualTo(2);
    }

    @Test
    void legacyCallbackStillInvokedForRuntimeMigration() {
        LegacyRecordingCallback callback = new LegacyRecordingCallback();
        cmmnEngineConfiguration.setCaseInstanceMigrationCallbacks(Collections.singletonList(callback));

        deployCaseDefinition("test1", ONE_TASK_CASE);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", ONE_TASK_CASE);

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .migrate(caseInstance.getId());

        assertThat(callback.getRuntimeTargetDefinitionIds()).containsExactly(destinationDefinition.getId());
    }

    static class RecordedMigration {

        private final CaseDefinition source;
        private final CaseDefinition target;

        RecordedMigration(CaseDefinition source, CaseDefinition target) {
            this.source = source;
            this.target = target;
        }

        public CaseDefinition getSource() {
            return source;
        }

        public CaseDefinition getTarget() {
            return target;
        }
    }

    static class RecordingCallback implements CaseInstanceMigrationCallback {

        private final List<RecordedMigration> runtimeMigrations = new ArrayList<>();
        private final List<RecordedMigration> historicMigrations = new ArrayList<>();

        @Override
        public void caseInstanceMigrated(CaseInstance caseInstance, CaseDefinition caseDefToMigrateTo, CaseInstanceMigrationDocument document) {
        }

        @Override
        public void historicCaseInstanceMigrated(HistoricCaseInstance caseInstance, CaseDefinition caseDefToMigrateTo,
                HistoricCaseInstanceMigrationDocument document) {
        }

        @Override
        public void caseInstanceMigrated(CaseInstance caseInstance, CaseDefinition sourceCaseDefinition,
                CaseDefinition caseDefToMigrateTo, CaseInstanceMigrationDocument document) {
            runtimeMigrations.add(new RecordedMigration(sourceCaseDefinition, caseDefToMigrateTo));
        }

        @Override
        public void historicCaseInstanceMigrated(HistoricCaseInstance caseInstance, CaseDefinition sourceCaseDefinition,
                CaseDefinition caseDefToMigrateTo, HistoricCaseInstanceMigrationDocument document) {
            historicMigrations.add(new RecordedMigration(sourceCaseDefinition, caseDefToMigrateTo));
        }

        public List<RecordedMigration> getRuntimeMigrations() {
            return runtimeMigrations;
        }

        public List<RecordedMigration> getHistoricMigrations() {
            return historicMigrations;
        }
    }

    /** Implements only the old methods to verify the new overloads delegate to them. */
    static class LegacyRecordingCallback implements CaseInstanceMigrationCallback {

        private final List<String> runtimeTargetDefinitionIds = new ArrayList<>();
        private final List<String> historicTargetDefinitionIds = new ArrayList<>();

        @Override
        public void caseInstanceMigrated(CaseInstance caseInstance, CaseDefinition caseDefToMigrateTo, CaseInstanceMigrationDocument document) {
            runtimeTargetDefinitionIds.add(caseDefToMigrateTo.getId());
        }

        @Override
        public void historicCaseInstanceMigrated(HistoricCaseInstance caseInstance, CaseDefinition caseDefToMigrateTo,
                HistoricCaseInstanceMigrationDocument document) {
            historicTargetDefinitionIds.add(caseDefToMigrateTo.getId());
        }

        public List<String> getRuntimeTargetDefinitionIds() {
            return runtimeTargetDefinitionIds;
        }

        public List<String> getHistoricTargetDefinitionIds() {
            return historicTargetDefinitionIds;
        }
    }

}
