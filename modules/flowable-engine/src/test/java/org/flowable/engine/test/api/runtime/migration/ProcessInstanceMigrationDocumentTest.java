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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationBuilderImpl;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationDocumentImpl;
import org.flowable.engine.impl.test.AbstractTestCase;
import org.flowable.engine.migration.ProcessInstanceActivityMigrationMapping;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.junit.jupiter.api.Test;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationDocumentTest extends AbstractTestCase {

    @Test
    public void testDeSerializeCompleteProcessInstanceMigrationDocument() {

        String definitionId = "someProcessId";
        String definitionKey = "MyProcessKey";
        Integer definitionVer = 9;
        String definitionTenantId = "admin";

        ProcessInstanceActivityMigrationMapping oneToOneMapping = ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity1", "newActivity1")
            .withLocalVariable("varForNewActivity1", "varValue")
            .withNewAssignee("kermit");

        ProcessInstanceActivityMigrationMapping manyToOneMapping = ProcessInstanceActivityMigrationMapping.createMappingFor(Arrays.asList("originalActivity3", "originalActivity4"), "newActivity3")
            .withLocalVariable("varForNewActivity3", 9876);

        ProcessInstanceActivityMigrationMapping oneToManyMapping = ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity2", Arrays.asList("newActivity2.1", "newActivity2.2"))
            .withLocalVariableForAllActivities("var1ForNewActivity2.x", "varValue")
            .withLocalVariableForAllActivities("var2ForNewActivity2.x", 1234.567)
            .withNewAssignee("the frog");

        HashMap<String, Map<String, Object>> activityLocalVariables = new HashMap<String, Map<String, Object>>() {

            {
                put("newActivity1", new HashMap<String, Object>() {

                    {
                        put("varForNewActivity1", "varValue");
                    }
                });
                put("newActivity3", new HashMap<String, Object>() {

                    {
                        put("varForNewActivity3", 9876);
                    }
                });
                put("newActivity2.1", new HashMap<String, Object>() {

                    {
                        put("var1ForNewActivity2.x", "varValue");
                        put("var2ForNewActivity2.x", 1234.567);
                    }
                });
                put("newActivity2.2", new HashMap<String, Object>() {

                    {
                        put("var1ForNewActivity2.x", "varValue");
                        put("var2ForNewActivity2.x", 1234.567);
                    }
                });
            }
        };

        HashMap<String, Object> processInstanceVariables = new HashMap<String, Object>() {

            {
                put("processVar1", "varValue1");
                put("processVar2", 456.789);
            }
        };

        String jsonAsStr = IoUtil.readFileAsString("org/flowable/engine/test/api/runtime/migration/completeProcessInstanceMigrationDocument.json");

        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentImpl.fromProcessInstanceMigrationDocumentJson(jsonAsStr);
        assertEquals(definitionId, migrationDocument.getMigrateToProcessDefinitionId());
        assertEquals(definitionKey, migrationDocument.getMigrateToProcessDefinitionKey());
        assertEquals(definitionVer, migrationDocument.getMigrateToProcessDefinitionVersion());
        assertEquals(definitionTenantId, migrationDocument.getMigrateToProcessDefinitionTenantId());
        assertThat(migrationDocument.getActivityMigrationMappings()).usingFieldByFieldElementComparator().containsExactly(oneToOneMapping, oneToManyMapping, manyToOneMapping);
        assertThat(migrationDocument.getActivitiesLocalVariables()).isEqualTo(activityLocalVariables);
        assertThat(migrationDocument.getProcessInstanceVariables()).isEqualTo(processInstanceVariables);
    }

    @Test
    public void testSerializeDeSerializeCompleteProcessInstanceMigrationDocumentForDefinitionId() {

        String definitionId = "someProcessId";

        HashMap<String, Object> newActivity2xVars = new HashMap<String, Object>() {

            {
                put("var1ForNewActivity2.x", "varValue");
                put("var2ForNewActivity2.x", 1234.567);
            }
        };

        HashMap<String, Map<String, Object>> activityLocalVariables = new HashMap<String, Map<String, Object>>() {

            {
                put("newActivity1", new HashMap<String, Object>() {

                    {
                        put("varForNewActivity1", "varValue");
                    }
                });
                put("newActivity3", new HashMap<String, Object>() {

                    {
                        put("varForNewActivity3", 9876);
                    }
                });
                put("newActivity2.1", new HashMap<String, Object>() {

                    {
                        put("var1ForNewActivity2.1", "varValue");
                        put("var2ForNewActivity2.1", 1234.567);
                    }
                });
                put("newActivity2.2", new HashMap<String, Object>() {

                    {
                        put("var1ForNewActivity2.2", "varValue");
                        put("var2ForNewActivity2.2", 1234.567);
                    }
                });
            }
        };

        Map<String, Object> varsForNewActivity2_1 = new HashMap<String, Object>() {

            {
                put("var1ForNewActivity2.1", "varValue");
                put("var2ForNewActivity2.1", 1234.567);
            }
        };

        Map<String, Object> varsForNewActivity2_2 = new HashMap<String, Object>() {

            {
                put("var1ForNewActivity2.2", "varValue");
                put("var2ForNewActivity2.2", 1234.567);
            }
        };

        HashMap<String, Object> processInstanceVariables = new HashMap<String, Object>() {

            {
                put("processVar1", "varValue1");
                put("processVar2", 456.789);
            }
        };
        ProcessInstanceActivityMigrationMapping.OneToOneMapping oneToOneMapping = ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity1", "newActivity1")
            .withLocalVariable("varForNewActivity1", "varValue")
            .withNewAssignee("kermit");
        ProcessInstanceActivityMigrationMapping.ManyToOneMapping manyToOneMapping = ProcessInstanceActivityMigrationMapping.createMappingFor(Arrays.asList("originalActivity3", "originalActivity4"), "newActivity3")
            .withLocalVariable("varForNewActivity3", 9876);
        ProcessInstanceActivityMigrationMapping.OneToManyMapping oneToManyMapping = ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity2", Arrays.asList("newActivity2.1", "newActivity2.2"))
            .withLocalVariablesForActivity("newActivity2.1", varsForNewActivity2_1)
            .withLocalVariablesForActivity("newActivity2.2", varsForNewActivity2_2)
            .withNewAssignee("the frog");

        ProcessInstanceMigrationDocument document = new ProcessInstanceMigrationBuilderImpl(null)
            .migrateToProcessDefinition(definitionId)
            .addActivityMigrationMapping(oneToOneMapping)
            .addActivityMigrationMapping(oneToManyMapping)
            .addActivityMigrationMapping(manyToOneMapping)
            .withProcessInstanceVariable("processVar1", "varValue1")
            .withProcessInstanceVariable("processVar2", 456.789)
            .getProcessInstanceMigrationDocument();

        //Serialize the document as Json
        String serializedDocument = document.asJsonString();

        //DeSerialize the document
        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentImpl.fromProcessInstanceMigrationDocumentJson(serializedDocument);

        assertEquals(definitionId, migrationDocument.getMigrateToProcessDefinitionId());
        assertNull(migrationDocument.getMigrateToProcessDefinitionKey());
        assertNull(migrationDocument.getMigrateToProcessDefinitionVersion());
        assertNull(migrationDocument.getMigrateToProcessDefinitionTenantId());
        assertThat(migrationDocument.getActivityMigrationMappings()).usingFieldByFieldElementComparator().containsAnyOf(oneToOneMapping, oneToManyMapping, manyToOneMapping);
        assertThat(migrationDocument.getActivitiesLocalVariables()).isEqualTo(activityLocalVariables);
        assertThat(migrationDocument.getProcessInstanceVariables()).isEqualTo(processInstanceVariables);
    }

    @Test
    public void testSerializeDuplicatedFromActivity() {

        String definitionId = "someProcessId";
        List<ProcessInstanceActivityMigrationMapping> activityMappings = new ArrayList<>();
        activityMappings.add(ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity1", "newActivity1"));
        activityMappings.add(ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity1", "newActivity2"));

        try {
            ProcessInstanceMigrationDocument document = new ProcessInstanceMigrationBuilderImpl(null)
                .migrateToProcessDefinition(definitionId)
                .addActivityMigrationMapping(ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity1", "newActivity1").withLocalVariable("varForNewActivity1", "varValue"))
                .addActivityMigrationMapping(ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity1", "newActivity2"))
                .addActivityMigrationMapping(ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity2", Arrays.asList("newActivity1", "newActivity2")))
                .addActivityMigrationMapping(ProcessInstanceActivityMigrationMapping.createMappingFor(Arrays.asList("originalActivity2", "originalActivity3"), "newActivity3"))
                .withProcessInstanceVariable("processVar1", "varValue1")
                .getProcessInstanceMigrationDocument();
            fail("Should not allow duplicated values in 'from' activity");
        } catch (FlowableException e) {
            assertTextPresent("From activity '[originalActivity1, originalActivity2]' is mapped more than once", e.getMessage());
        }
    }

    @Test
    public void testDeSerializeDuplicatedFromActivity() {

        String jsonAsStr = IoUtil.readFileAsString("org/flowable/engine/test/api/runtime/migration/duplicatedFromActivitiesInFromMappingMigrationDocument.json");

        try {
            ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentImpl.fromProcessInstanceMigrationDocumentJson(jsonAsStr);
            fail("Should not allow duplicated values in 'from' activity");
        } catch (FlowableException e) {
            assertTextPresent("From activity '[originalActivity1, originalActivity2]' is mapped more than once", e.getMessage());
        }
    }

    @Test
    public void testSerializeDeSerializeProcessInstanceMigrationDocumentForDefinitionKeyVersion() {

        String definitionKey = "MyProcessKey";
        Integer definitionVer = 5;
        String definitionTenantId = "admin";
        ProcessInstanceActivityMigrationMapping.OneToOneMapping oneToOne1 = ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity1", "newActivity1");
        ProcessInstanceActivityMigrationMapping.OneToOneMapping oneToOne2 = ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity2", "newActivity2");

        //Build a process migration document
        ProcessInstanceMigrationDocument document = new ProcessInstanceMigrationBuilderImpl(null)
            .migrateToProcessDefinition(definitionKey, definitionVer)
            .withMigrateToProcessDefinitionTenantId(definitionTenantId)
            .addActivityMigrationMapping(ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity1", "newActivity1"))
            .addActivityMigrationMapping(ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity2", "newActivity2"))
            .getProcessInstanceMigrationDocument();

        //Serialize the document as Json
        String serializedDocument = document.asJsonString();

        //DeSerialize the document
        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentImpl.fromProcessInstanceMigrationDocumentJson(serializedDocument);

        assertNull(migrationDocument.getMigrateToProcessDefinitionId());
        assertEquals(definitionKey, migrationDocument.getMigrateToProcessDefinitionKey());
        assertEquals(definitionVer, migrationDocument.getMigrateToProcessDefinitionVersion());
        assertEquals(definitionTenantId, migrationDocument.getMigrateToProcessDefinitionTenantId());
        assertThat(migrationDocument.getActivityMigrationMappings()).usingFieldByFieldElementComparator().containsExactly(oneToOne1, oneToOne2);
    }

    @Test
    public void testSerializeDeSerializeProcessInstanceMigrationDocumentWithVariables() {
        String definitionKey = "MyProcessKey";
        Integer definitionVer = 5;
        String definitionTenantId = "admin";

        ProcessInstanceActivityMigrationMapping.OneToOneMapping oneToOne1 = ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity1", "newActivity1")
            .withLocalVariables(Collections.singletonMap("variableString", "variableValue"));
        ProcessInstanceActivityMigrationMapping.OneToOneMapping oneToOne2 = ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity2", "newActivity2")
            .withLocalVariable("variableDouble", 12345.6789);

        HashMap processInstanceVars = new HashMap<String, Object>() {

            {
                put("instanceVar1", "stringValue");
                put("instanceVar2", 12345.6789);
            }
        };

        //Build a process migration document
        ProcessInstanceMigrationDocument document = new ProcessInstanceMigrationBuilderImpl(null)
            .migrateToProcessDefinition(definitionKey, definitionVer)
            .withMigrateToProcessDefinitionTenantId(definitionTenantId)
            .addActivityMigrationMapping(ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity1", "newActivity1").withLocalVariables(Collections.singletonMap("variableString", "variableValue")))
            .addActivityMigrationMapping(ProcessInstanceActivityMigrationMapping.createMappingFor("originalActivity2", "newActivity2").withLocalVariable("variableDouble", 12345.6789))
            .withProcessInstanceVariable("instanceVar1", "stringValue")
            .withProcessInstanceVariable("instanceVar2", 12345.6789)
            .getProcessInstanceMigrationDocument();

        //Serialize the document as Json
        String serializedDocument = document.asJsonString();

        //DeSerialize the document
        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentImpl.fromProcessInstanceMigrationDocumentJson(serializedDocument);

        assertNull(migrationDocument.getMigrateToProcessDefinitionId());
        assertEquals(definitionKey, migrationDocument.getMigrateToProcessDefinitionKey());
        assertEquals(definitionVer, migrationDocument.getMigrateToProcessDefinitionVersion());
        assertEquals(definitionTenantId, migrationDocument.getMigrateToProcessDefinitionTenantId());
        assertThat(migrationDocument.getActivityMigrationMappings()).usingFieldByFieldElementComparator().containsExactly(oneToOne1, oneToOne2);
        assertThat(migrationDocument.getActivitiesLocalVariables()).containsKeys("newActivity1", "newActivity2");
        assertThat(migrationDocument.getActivitiesLocalVariables().get("newActivity1")).isEqualTo((Collections.singletonMap("variableString", "variableValue")));
        assertThat(migrationDocument.getActivitiesLocalVariables().get("newActivity2")).isEqualTo((Collections.singletonMap("variableDouble", 12345.6789)));
        assertThat(migrationDocument.getProcessInstanceVariables()).isEqualTo(processInstanceVars);
    }
}
