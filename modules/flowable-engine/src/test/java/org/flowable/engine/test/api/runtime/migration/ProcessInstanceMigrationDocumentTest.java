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

import java.util.HashMap;

import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationDocumentImpl;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.junit.jupiter.api.Test;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationDocumentTest extends PluggableFlowableTestCase {

    @Test
    public void testDeSerializeProcessInstanceMigrationDocument() {

        String definitionId = "someProcessId";
        String definitionKey = "MyProcessKey";
        int definitionVer = 9;
        String definitionTenantId = "admin";
        HashMap<String, String> activityMappings = new HashMap<String, String>() {

            {
                put("originalActivity1", "newActivity1");
                put("originalActivity2", "newActivity2");
            }
        };

        String jsonAsStr = IoUtil.readFileAsString("org/flowable/engine/test/api/runtime/migration/simpleProcessInstanceMigrationDocument.json");

        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentImpl.fromProcessInstanceMigrationDocumentJson(jsonAsStr);

        assertEquals(definitionId, migrationDocument.getMigrateToProcessDefinitionId());
        assertEquals(definitionKey, migrationDocument.getMigrateToProcessDefinitionKey());
        assertEquals(definitionVer, migrationDocument.getMigrateToProcessDefinitionVersion());
        assertEquals(definitionTenantId, migrationDocument.getMigrateToProcessDefinitionTenantId());
        assertThat(migrationDocument.getActivityMigrationMappings(), is(activityMappings));

    }

    @Test
    public void testSerializeDeSerializeProcessInstanceMigrationDocumentForDefinitionId() {

        String definitionId = "someProcessId";
        HashMap<String, String> activityMappings = new HashMap<String, String>() {
            {
                put("originalActivity1", "newActivity1");
                put("originalActivity2", "newActivity2");
            }
        };

        ProcessInstanceMigrationDocument document = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(definitionId)
            .addActivityMigrationMapping("originalActivity2", "newActivity2")
            .addActivityMigrationMappings(activityMappings)
            .getProcessInstanceMigrationDocument();

        //Serialize the document as Json
        String serializedDocument = document.asJsonString();

        //DeSerialize the document
        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentImpl.fromProcessInstanceMigrationDocumentJson(serializedDocument);

        assertEquals(definitionId, migrationDocument.getMigrateToProcessDefinitionId());
        assertNull(migrationDocument.getMigrateToProcessDefinitionKey());
        assertEquals(0, migrationDocument.getMigrateToProcessDefinitionVersion());
        assertNull(migrationDocument.getMigrateToProcessDefinitionTenantId());
        assertThat(migrationDocument.getActivityMigrationMappings(), is(activityMappings));
    }

    @Test
    public void testSerializeDeSerializeProcessInstanceMigrationDocumentForDefinitionKeyVersion() {

        String definitionKey = "MyProcessKey";
        int definitionVer = 5;
        String definitionTenantId = "admin";
        HashMap<String, String> activityMappings = new HashMap<String, String>() {

            {
                put("originalActivity1", "newActivity1");
                put("originalActivity2", "newActivity2");
            }
        };

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

        assertNull(migrationDocument.getMigrateToProcessDefinitionId());
        assertEquals(definitionKey, migrationDocument.getMigrateToProcessDefinitionKey());
        assertEquals(definitionVer, migrationDocument.getMigrateToProcessDefinitionVersion());
        assertEquals(definitionTenantId, migrationDocument.getMigrateToProcessDefinitionTenantId());
        assertThat(migrationDocument.getActivityMigrationMappings(), is(activityMappings));
    }
}
