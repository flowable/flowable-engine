package org.flowable.engine.impl.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationDocumentBuilder;

public class ProcessInstanceMigrationDocumentBuilderImpl implements ProcessInstanceMigrationDocumentBuilder {

    protected String migrateToProcessDefinitionId;
    protected String migrateToProcessDefinitionKey;
    protected String migrateToProcessDefinitionVersion;
    protected String migrateToProcessDefinitionTenantId;
    protected Set<String> processInstancesIdsToMigrate = new HashSet<>();
    protected Map<String, String> activityMigrationMappings = new HashMap<>();

    public static ProcessInstanceMigrationDocumentBuilder fromProcessInstanceMigrationDocument(ProcessInstanceMigrationDocument document) {
        ProcessInstanceMigrationDocumentBuilderImpl builder = new ProcessInstanceMigrationDocumentBuilderImpl();
        builder.migrateToProcessDefinitionId = document.getMigrateToProcessDefinitionId();
        builder.migrateToProcessDefinitionKey = document.getMigrateToProcessDefinitionKey();
        builder.migrateToProcessDefinitionVersion = document.getMigrateToProcessDefinitionVersion();
        builder.processInstancesIdsToMigrate = new HashSet<>(document.getProcessInstancesIdsToMigrate());
        builder.activityMigrationMappings = document.getActivityMigrationMappings();
        return builder;
    }

    public static ProcessInstanceMigrationDocumentBuilder fromProcessInstanceMigrationDocumentJson(String processInstanceMigrationDocumentJson) {
        ProcessInstanceMigrationDocument document = ProcessInstanceMigrationDocumentImpl.fromProcessInstanceMigrationDocumentJson(processInstanceMigrationDocumentJson);
        return fromProcessInstanceMigrationDocument(document);
    }

    @Override
    public ProcessInstanceMigrationDocumentBuilder setProcessDefinitionToMigrateTo(String processDefinitionId) {
        this.migrateToProcessDefinitionId = processDefinitionId;
        return this;
    }

    @Override
    public ProcessInstanceMigrationDocumentBuilder setProcessDefinitionToMigrateTo(String processDefinitionKey, String processDefinitionVersion) {
        this.migrateToProcessDefinitionKey = processDefinitionKey;
        this.migrateToProcessDefinitionVersion = processDefinitionVersion;
        return this;
    }

    @Override
    public ProcessInstanceMigrationDocumentBuilder setProcessDefinitionToMigrateTo(String processDefinitionKey, String processDefinitionVersion, String processDefinitionTenantId) {
        this.migrateToProcessDefinitionKey = processDefinitionKey;
        this.migrateToProcessDefinitionVersion = processDefinitionVersion;
        this.migrateToProcessDefinitionTenantId = processDefinitionTenantId;
        return this;
    }

    @Override
    public ProcessInstanceMigrationDocumentBuilder setTenantOfProcessDefinitionToMigrateTo(String processDefinitionTenantId) {
        this.migrateToProcessDefinitionTenantId = processDefinitionTenantId;
        return this;
    }

    @Override
    public ProcessInstanceMigrationDocumentBuilder addProcessInstanceIdToMigrate(String processInstanceId) {
        Objects.requireNonNull(processInstanceId, "process instance id to migrate cannot be null");
        this.processInstancesIdsToMigrate.add(processInstanceId);
        return this;
    }

    @Override
    public ProcessInstanceMigrationDocumentBuilder addProcessInstancesIdsToMigrate(String... processInstancesIds) {
        Objects.requireNonNull(processInstancesIds);
        this.processInstancesIdsToMigrate.addAll(Arrays.asList(processInstancesIds));
        return this;
    }

    @Override
    public ProcessInstanceMigrationDocumentBuilder addActivityMigrationMappings(Map<String, String> activityMigrationMappings) {
        this.activityMigrationMappings.putAll(activityMigrationMappings);
        return this;
    }

    @Override
    public ProcessInstanceMigrationDocumentBuilder addActivityMigrationMapping(String fromActivityId, String toActivityId) {
        Objects.requireNonNull(fromActivityId, "From process activity cannot be null");
        Objects.requireNonNull(toActivityId, "To process activity cannot be null");
        this.activityMigrationMappings.put(fromActivityId, toActivityId);
        return this;
    }

    @Override
    public ProcessInstanceMigrationDocument build() {

        if (migrateToProcessDefinitionId == null) {
            Objects.requireNonNull(migrateToProcessDefinitionKey, "Process definition key cannot be null");
            Objects.requireNonNull(migrateToProcessDefinitionVersion, "Process definition version cannot be null");
        }

        ProcessInstanceMigrationDocumentImpl document = new ProcessInstanceMigrationDocumentImpl();
        document.setMigrateToProcessDefinitionId(migrateToProcessDefinitionId);
        document.setMigrateToProcessDefinition(migrateToProcessDefinitionKey, migrateToProcessDefinitionVersion, migrateToProcessDefinitionTenantId);
        document.setActivityMigrationMappings(activityMigrationMappings);
        document.setProcessInstancesIdsToMigrate(new ArrayList<>(processInstancesIdsToMigrate));

        return document;
    }

}
