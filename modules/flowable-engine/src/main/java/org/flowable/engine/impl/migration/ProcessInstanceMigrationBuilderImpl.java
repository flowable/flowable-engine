package org.flowable.engine.impl.migration;

import java.util.Map;

import org.flowable.engine.impl.RuntimeServiceImpl;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationDocumentBuilder;

public class ProcessInstanceMigrationBuilderImpl implements ProcessInstanceMigrationBuilder {

    protected RuntimeServiceImpl runtimeService;
    protected ProcessInstanceMigrationDocumentBuilder migrationDocumentBuilder;

    public static ProcessInstanceMigrationBuilder fromProcessInstanceMigrationDocument(RuntimeServiceImpl runtimeService, ProcessInstanceMigrationDocument document) {
        ProcessInstanceMigrationDocumentBuilder documentBuilder = ProcessInstanceMigrationDocumentBuilderImpl.fromProcessInstanceMigrationDocument(document);
        ProcessInstanceMigrationBuilderImpl migrationBuilder = new ProcessInstanceMigrationBuilderImpl(runtimeService);
        migrationBuilder.migrationDocumentBuilder = documentBuilder;
        return migrationBuilder;
    }

    public ProcessInstanceMigrationBuilderImpl(RuntimeServiceImpl runtimeService) {
        this.runtimeService = runtimeService;
        this.migrationDocumentBuilder = new ProcessInstanceMigrationDocumentBuilderImpl();
    }

    @Override
    public ProcessInstanceMigrationBuilder migrateToProcessDefinition(String processDefinitionId) {
        this.migrationDocumentBuilder.setProcessDefinitionToMigrateTo(processDefinitionId);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder migrateToProcessDefinition(String processDefinitionKey, String processDefinitionVersion) {
        this.migrationDocumentBuilder.setProcessDefinitionToMigrateTo(processDefinitionKey, processDefinitionVersion);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder migrateToProcessDefinition(String processDefinitionKey, String processDefinitionVersion, String processDefinitionTenantId) {
        this.migrationDocumentBuilder.setProcessDefinitionToMigrateTo(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder withMigrateToProcessDefinitionTenantId(String processDefinitionTenantId) {
        this.migrationDocumentBuilder.setTenantOfProcessDefinitionToMigrateTo(processDefinitionTenantId);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder addProcessInstanceToMigrate(String processInstanceId) {
        this.migrationDocumentBuilder.addProcessInstanceIdToMigrate(processInstanceId);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder addProcessInstancesToMigrate(String... processInstanceIds) {
        this.migrationDocumentBuilder.addProcessInstancesIdsToMigrate(processInstanceIds);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder addActivityMigrationMapping(String fromActivityId, String toActivityId) {
        this.migrationDocumentBuilder.addActivityMigrationMapping(fromActivityId, toActivityId);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder addActivityMigrationMappings(Map<String, String> activityMigrationMappings) {
        this.migrationDocumentBuilder.addActivityMigrationMappings(activityMigrationMappings);
        return this;
    }

    @Override
    public ProcessInstanceMigrationDocument getProcessInstanceMigrationDocument() {
        return this.migrationDocumentBuilder.build();
    }

    @Override
    public void migrate() {
        ProcessInstanceMigrationDocument document = this.migrationDocumentBuilder.build();
        runtimeService.migrateProcessInstance(document);
    }

}
