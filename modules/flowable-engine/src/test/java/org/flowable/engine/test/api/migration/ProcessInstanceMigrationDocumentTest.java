package org.flowable.engine.test.api.migration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.engine.impl.migration.ProcessInstanceMigrationDocumentImpl;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;

public class ProcessInstanceMigrationDocumentTest extends PluggableFlowableTestCase {

    public void testDeSerializeProcessInstanceMigrationDocument() {

        String definitionId = "someProcessId";
        String definitionKey = "MyProcessKey";
        String definitionVer = "version 1";
        String definitionTenantId = "admin";
        List<String> instancesIds = Arrays.asList("123", "234", "567");
        Map<String, String> activityMappings = Stream.of(new String[][] {
            { "originalActivity1", "newActivity1" },
            { "originalActivity2", "newActivity2" }
        }).collect(Collectors.toMap(a -> a[0], a -> a[1]));

        String sb = "{"
            + "\"migrateToProcessDefinitionId\":" + "\"" + definitionId + "\"" + ","
            + "\"migrateToProcessDefinitionKey\":" + "\"" + definitionKey + "\"" + ","
            + "\"migrateToProcessDefinitionVersion\":" + "\"" + definitionVer + "\"" + ","
            + "\"migrateToProcessDefinitionTenantId\":" + "\"" + definitionTenantId + "\"" + ","
            + "\"nonMappedProperty\": \"someValue\"" + ","
            + "\"processInstancesIdsToMigrate\": [ \"123\"" + "," + "\"234\"" + "," + "\"567\" ]" + ","
            + "\"activityMigrationMappings\": {\"originalActivity1\": \"newActivity1\", \"originalActivity2\": \"newActivity2\"}"
            + "}";
        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentImpl.fromProcessInstanceMigrationDocumentJson(sb);

        assertEquals(definitionId, migrationDocument.getMigrateToProcessDefinitionId());
        assertEquals(definitionKey, migrationDocument.getMigrateToProcessDefinitionKey());
        assertEquals(definitionVer, migrationDocument.getMigrateToProcessDefinitionVersion());
        assertEquals(definitionTenantId, migrationDocument.getMigrateToProcessDefinitionTenantId());
        assertThat(migrationDocument.getProcessInstancesIdsToMigrate(), is(instancesIds));
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
            .addProcessInstanceToMigrate("234")
            .addProcessInstancesToMigrate("123", "567")
            .addActivityMigrationMapping("originalActivity2", "newActivity2")
            .addActivityMigrationMappings(activityMappings)
            .getProcessInstanceMigrationDocument();

        //Serialize the document as Json
        String serializedDocument = document.asJsonString();

        //DeSerialize the document
        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentImpl.fromProcessInstanceMigrationDocumentJson(serializedDocument);

        assertEquals(definitionId, migrationDocument.getMigrateToProcessDefinitionId());
        assertNull(migrationDocument.getMigrateToProcessDefinitionKey());
        assertNull(migrationDocument.getMigrateToProcessDefinitionVersion());
        assertNull(migrationDocument.getMigrateToProcessDefinitionTenantId());
        assertThat(migrationDocument.getProcessInstancesIdsToMigrate(), is(instancesIds));
        assertThat(migrationDocument.getActivityMigrationMappings(), is(activityMappings));
    }

    public void testSerializeDeSerializeProcessInstanceMigrationDocumentForDefinitionKeyVersion() {

        String definitionKey = "MyProcessKey";
        String definitionVer = "version 1";
        String definitionTenantId = "admin";
        List<String> instancesIds = Arrays.asList("123", "234", "567");
        Map<String, String> activityMappings = Stream.of(new String[][] {
            { "originalActivity1", "newActivity1" },
            { "originalActivity2", "newActivity2" }
        }).collect(Collectors.toMap(a -> a[0], a -> a[1]));

        //Build a process migration document
        ProcessInstanceMigrationDocument document = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(definitionKey, definitionVer)
            .withMigrateToProcessDefinitionTenantId(definitionTenantId)
            .addProcessInstanceToMigrate("234")
            .addProcessInstancesToMigrate("123", "567")
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
        assertThat(migrationDocument.getProcessInstancesIdsToMigrate(), is(instancesIds));
        assertThat(migrationDocument.getActivityMigrationMappings(), is(activityMappings));
    }
}
