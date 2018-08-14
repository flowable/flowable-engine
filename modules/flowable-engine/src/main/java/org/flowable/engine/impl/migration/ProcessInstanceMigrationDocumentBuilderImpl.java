package org.flowable.engine.impl.migration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationDocumentBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ProcessInstanceMigrationDocumentBuilderImpl implements ProcessInstanceMigrationDocumentBuilder {

    protected String migrateToProcessDefinitionId;
    protected String migrateToProcessDefinitionKey;
    protected String migrateToProcessDefinitionVersion;
    protected Set<String> processInstancesIdsToMigrate = new HashSet<>();
    protected Map<String, String> activityMigrationMappings = new HashMap<>();

    public ProcessInstanceMigrationDocumentBuilderImpl(String processDefinitionId) {
        Objects.requireNonNull(processDefinitionId, "Process definition id cannot be null");
        this.migrateToProcessDefinitionId = processDefinitionId;
    }

    public ProcessInstanceMigrationDocumentBuilderImpl(String processDefinitionKey, String processDefinitionVersion) {
        Objects.requireNonNull(processDefinitionKey, "Process definition key cannot be null");
        Objects.requireNonNull(processDefinitionVersion, "Process definition version cannot be null");
        this.migrateToProcessDefinitionKey = processDefinitionKey;
        this.migrateToProcessDefinitionVersion = processDefinitionVersion;
    }

    //    @Override
    //    public ProcessInstanceMigrationDocumentBuilder toProcessDefinitionId(String processDefinitionId) {
    //        this.migrateToProcessDefinitionId = processDefinitionId;
    //        return this;
    //    }
    //
    //    @Override
    //    public ProcessInstanceMigrationDocumentBuilder toProcessDefinitionVersion(String processDefinitionKey, String processDefinitionVersion) {
    //        this.migrateToProcessDefinitionKey = processDefinitionKey;
    //        this.migrateToProcessDefinitionVersion = processDefinitionVersion;
    //        return this;
    //    }

    @Override
    public ProcessInstanceMigrationDocumentBuilder addProcessInstanceIdToMigrate(String processInstanceId) {
        Objects.requireNonNull(processInstanceId, "process instance id to migrate cannot be null");
        this.processInstancesIdsToMigrate.add(processInstanceId);
        return this;
    }

    @Override
    public ProcessInstanceMigrationDocumentBuilder addProcessInstancesIdsToMigrate(String... processInstancesIds) {
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

        ProcessInstanceMigrationDocumentImpl document = new ProcessInstanceMigrationDocumentImpl();
        document.setMigrateToProcessDefinitionId(migrateToProcessDefinitionId);
        document.setMigrateToProcessDefinitionKey(migrateToProcessDefinitionKey);
        document.setMigrateToProcessDefinitionVersion(migrateToProcessDefinitionVersion);
        document.setActivityMigrationMappings(activityMigrationMappings);
        document.setProcessInstancesIdsToMigrate(new ArrayList<>(processInstancesIdsToMigrate));

        return document;
    }

    public static ProcessInstanceMigrationDocument buildFromJson(String processInstanceMigrationDocumentJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(processInstanceMigrationDocumentJson, ProcessInstanceMigrationDocumentImpl.class);
        } catch (IOException e) {
            throw new FlowableIllegalArgumentException("Low level I/O problem with Json argument", e);
        }
    }

}
