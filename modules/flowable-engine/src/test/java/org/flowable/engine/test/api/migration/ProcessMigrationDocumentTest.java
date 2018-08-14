package org.flowable.engine.test.api.migration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.engine.impl.migration.ProcessInstanceMigrationDocumentImpl;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ProcessMigrationDocumentTest extends PluggableFlowableTestCase {

    public void testSerializeDeSerializeProcessInstanceMigrationDocumentForDefinitionId() throws IOException {

        String definitionId = "someProcessId";
        List<String> instancesIds = Arrays.asList("123", "234", "567");
        Map<String, String> activityMappings = Stream.of(new String[][] {
            { "originalActivity1", "newActivity1" },
            { "originalActivity2", "newActivity2" }
        }).collect(Collectors.toMap(a -> a[0], a -> a[1]));

        //Build a process migration document
        ProcessInstanceMigrationDocument document = runtimeService.createProcessInstanceMigrationDocumentBuilder(definitionId)
            .addProcessInstanceIdToMigrate("234")
            .addProcessInstancesIdsToMigrate("123", "567")
            .addActivityMigrationMapping("originalActivity2", "newActivity2")
            .addActivityMigrationMappings(activityMappings)
            .build();

        //Serialize the document as Json
        String serializedDocument = document.toJsonString();


        //DeSerialize the document
        ObjectMapper objectMapper = new ObjectMapper();
        ProcessInstanceMigrationDocument migrationDocument = objectMapper.readValue(serializedDocument, ProcessInstanceMigrationDocumentImpl.class);

        assertEquals(definitionId, migrationDocument.getMigrateToProcessDefinitionId());
        assertNull(migrationDocument.getMigrateToProcessDefinitionKey());
        assertNull(migrationDocument.getMigrateToProcessDefinitionVersion());
        assertThat(migrationDocument.getProcessInstancesIdsToMigrate(), is(instancesIds));
        assertThat(migrationDocument.getActivityMigrationMappings(), is(activityMappings));
    }

    public void testSerializeDeSerializeProcessInstanceMigrationDocumentForDefinitionKeyVersion() throws IOException {

        String definitionKey = "MyProcessKey";
        String definitionVer = "version 1";
        List<String> instancesIds = Arrays.asList("123", "234", "567");
        Map<String, String> activityMappings = Stream.of(new String[][] {
            { "originalActivity1", "newActivity1" },
            { "originalActivity2", "newActivity2" }
        }).collect(Collectors.toMap(a -> a[0], a -> a[1]));

        //Build a process migration document
        ProcessInstanceMigrationDocument document = runtimeService.createProcessInstanceMigrationDocumentBuilder(definitionKey, definitionVer)
            .addProcessInstanceIdToMigrate("234")
            .addProcessInstancesIdsToMigrate("123", "567")
            .addActivityMigrationMapping("originalActivity2", "newActivity2")
            .addActivityMigrationMappings(activityMappings)
            .build();

        //Serialize the document as Json
        String serializedDocument = document.toJsonString();


        //DeSerialize the document
        ObjectMapper objectMapper = new ObjectMapper();
        ProcessInstanceMigrationDocument migrationDocument = objectMapper.readValue(serializedDocument, ProcessInstanceMigrationDocumentImpl.class);

        assertNull(migrationDocument.getMigrateToProcessDefinitionId());
        assertEquals(definitionKey, migrationDocument.getMigrateToProcessDefinitionKey());
        assertEquals(definitionVer, migrationDocument.getMigrateToProcessDefinitionVersion());
        assertThat(migrationDocument.getProcessInstancesIdsToMigrate(), is(instancesIds));
        assertThat(migrationDocument.getActivityMigrationMappings(), is(activityMappings));
    }

    public void testDeSerializeProcessInstanceMigrationDocument() {

        String definitionId = "someProcessId";
        String definitionKey = "MyProcessKey";
        String definitionVer = "version 1";
        List<String> instancesIds = Arrays.asList("123", "234", "567");
        Map<String, String> activityMappings = Stream.of(new String[][] {
            { "originalActivity1", "newActivity1" },
            { "originalActivity2", "newActivity2" }
        }).collect(Collectors.toMap(a -> a[0], a -> a[1]));

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"migrateToProcessDefinitionId\": \"someProcessId\"").append(",");
        sb.append("\"migrateToProcessDefinitionKey\": \"MyProcessKey\"").append(",");
        sb.append("\"migrateToProcessDefinitionVersion\": \"version 1\"").append(",");
        sb.append("\"processInstancesIdsToMigrate\": [ \"123\"").append(",").append("\"234\"").append(",").append("\"567\" ]").append(",");
        sb.append("\"activityMigrationMappings\": {\"originalActivity1\": \"newActivity1\", \"originalActivity2\": \"newActivity2\"}");
        sb.append("}");

        ProcessInstanceMigrationDocument migrationDocument = runtimeService.createProcessInstanceMigrationDocumentFromJson(sb.toString());

        assertEquals(definitionId, migrationDocument.getMigrateToProcessDefinitionId());
        assertEquals(definitionKey, migrationDocument.getMigrateToProcessDefinitionKey());
        assertEquals(definitionVer, migrationDocument.getMigrateToProcessDefinitionVersion());
        assertThat(migrationDocument.getProcessInstancesIdsToMigrate(), is(instancesIds));
        assertThat(migrationDocument.getActivityMigrationMappings(), is(activityMappings));

    }

    //    public void testCompareModels() {
    //        Deployment version1 = repositoryService.createDeployment()
    //            .name("My Process Deployment")
    //            .addClasspathResource("org/flowable/engine/test/api/runtime/migration/MyProcess-v1.bpmn20.xml")
    //            .deploy();
    //
    //
    //        Deployment version2 = repositoryService.createDeployment()
    //            .name("My Process Deployment")
    //            .addClasspathResource("org/flowable/engine/test/api/runtime/migration/MyProcess-v2.bpmn20.xml")
    //            .deploy();
    //
    //        List<Model> models = repositoryService.createModelQuery().list();
    //
    //        List<DeploymentEntity> deployments = repositoryService.createDeploymentQuery().list().stream().map(d -> (DeploymentEntity) d).collect(Collectors.toList());
    //
    //        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");
    //
    //        BpmnModel bpmnModel = repositoryService.getBpmnModel("MP");
    //
    //        System.out.printf("done");
    //
    //    }

}
