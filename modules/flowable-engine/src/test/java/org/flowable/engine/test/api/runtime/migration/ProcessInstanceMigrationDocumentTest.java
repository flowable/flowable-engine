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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationDocumentImpl;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationDocumentTest extends PluggableFlowableTestCase {

    public void testDeSerializeProcessInstanceMigrationDocument() {

        String definitionId = "someProcessId";
        String definitionKey = "MyProcessKey";
        String definitionVer = "version 1";
        String definitionTenantId = "admin";
        Map<String, String> activityMappings = Stream.of(new String[][] {
            { "originalActivity1", "newActivity1" },
            { "originalActivity2", "newActivity2" }
        }).collect(Collectors.toMap(a -> a[0], a -> a[1]));

        String jsonAsStr = IoUtil.readFileAsString("org/flowable/engine/test/api/runtime/migration/simpleProcessInstanceMigrationDocument.json");

        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentImpl.fromProcessInstanceMigrationDocumentJson(jsonAsStr);

        assertEquals(definitionId, migrationDocument.getMigrateToProcessDefinitionId().get());
        assertEquals(definitionKey, migrationDocument.getMigrateToProcessDefinitionKey().get());
        assertEquals(definitionVer, migrationDocument.getMigrateToProcessDefinitionVersion().get());
        assertEquals(definitionTenantId, migrationDocument.getMigrateToProcessDefinitionTenantId().get());
        assertThat(migrationDocument.getActivityMigrationMappings(), is(activityMappings));

    }

    public void testSerializeDeSerializeProcessInstanceMigrationDocumentForDefinitionId() {

        String definitionId = "someProcessId";
        List<String> instancesIds = Arrays.asList("123", "234", "567");
        Map<String, String> activityMappings = Stream.of(new String[][] {
            { "originalActivity1", "newActivity1" },
            { "originalActivity2", "newActivity2" }
        }).collect(Collectors.toMap(a -> a[0], a -> a[1]));

        ProcessInstanceMigrationDocument document = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(definitionId)
            .addActivityMigrationMapping("originalActivity2", "newActivity2")
            .addActivityMigrationMappings(activityMappings)
            .getProcessInstanceMigrationDocument();

        //Serialize the document as Json
        String serializedDocument = document.asJsonString();

        //DeSerialize the document
        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentImpl.fromProcessInstanceMigrationDocumentJson(serializedDocument);

        assertEquals(definitionId, migrationDocument.getMigrateToProcessDefinitionId().get());
        assertFalse(migrationDocument.getMigrateToProcessDefinitionKey().isPresent());
        assertFalse(migrationDocument.getMigrateToProcessDefinitionVersion().isPresent());
        assertFalse(migrationDocument.getMigrateToProcessDefinitionTenantId().isPresent());
        assertThat(migrationDocument.getActivityMigrationMappings(), is(activityMappings));
    }

    public void testSerializeDeSerializeProcessInstanceMigrationDocumentForDefinitionKeyVersion() {

        String definitionKey = "MyProcessKey";
        String definitionVer = "version 1";
        String definitionTenantId = "admin";
        Map<String, String> activityMappings = Stream.of(new String[][] {
            { "originalActivity1", "newActivity1" },
            { "originalActivity2", "newActivity2" }
        }).collect(Collectors.toMap(a -> a[0], a -> a[1]));

        //Build a process migration document
        ProcessInstanceMigrationDocument document = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(definitionKey, definitionVer)
            .withMigrateToProcessDefinitionTenantId(definitionTenantId)
            .addActivityMigrationMapping("originalActivity2", "newActivity2")
            .addActivityMigrationMappings(activityMappings)
            .getProcessInstanceMigrationDocument();

        //Serialize the document as Json
        String serializedDocument = document.asJsonString();

        //DeSerialize the document
        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentImpl.fromProcessInstanceMigrationDocumentJson(serializedDocument);

        assertFalse(migrationDocument.getMigrateToProcessDefinitionId().isPresent());
        assertEquals(definitionKey, migrationDocument.getMigrateToProcessDefinitionKey().get());
        assertEquals(definitionVer, migrationDocument.getMigrateToProcessDefinitionVersion().get());
        assertEquals(definitionTenantId, migrationDocument.getMigrateToProcessDefinitionTenantId().get());
        assertThat(migrationDocument.getActivityMigrationMappings(), is(activityMappings));
    }
}
