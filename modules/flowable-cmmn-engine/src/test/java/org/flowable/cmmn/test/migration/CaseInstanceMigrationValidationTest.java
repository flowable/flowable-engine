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

import org.flowable.cmmn.api.migration.CaseInstanceMigrationValidationResult;
import org.flowable.cmmn.api.migration.PlanItemDefinitionMappingBuilder;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Valentin Zickner
 */
public class CaseInstanceMigrationValidationTest extends AbstractCaseMigrationTest {

    @Test
    void withSimpleOneTaskCase() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");

        // Act
        CaseInstanceMigrationValidationResult result = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .validateMigration(caseInstance.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void withNotExistingDestinationDefinition() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();

        // Act
        CaseInstanceMigrationValidationResult result = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition("NOT_EXISTING_CASE_DEFINITION_ID")
                .validateMigration(caseInstance.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getValidationMessages())
                .containsExactly("Cannot find the case definition to migrate to, with [id:'NOT_EXISTING_CASE_DEFINITION_ID']");
    }

    @Test
    void withManualMappingAndWrongDestination() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");

        // Act
        CaseInstanceMigrationValidationResult result = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("planItem3"))
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("planItem2"))
                .validateMigration(caseInstance.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getValidationMessages())
                .containsExactlyInAnyOrder("Invalid mapping for terminate plan item definition 'planItem2' cannot be found in the case definition",
                        "Invalid mapping for activate plan item definition 'planItem3' cannot be found in the case definition");
    }

    @Test
    void withValidOneToOneMapping() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/three-task.cmmn.xml");

        // Act
        CaseInstanceMigrationValidationResult result = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("planItem3"))
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("planItem2"))
                .validateMigration(caseInstance.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.hasErrors()).isFalse();
    }

}
