package org.flowable.engine.impl.migration;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProcessInstanceMigrationDocumentImpl implements ProcessInstanceMigrationDocument {

    protected String migrateToProcessDefinitionId;
    protected String migrateToProcessDefinitionKey;
    protected String migrateToProcessDefinitionVersion;
    protected String migrateToProcessDefinitionTenantId;
    protected List<String> processInstancesIdsToMigrate;
    protected Map<String, String> activityMigrationMappings;

    public static ProcessInstanceMigrationDocument fromProcessInstanceMigrationDocumentJson(String processInstanceMigrationDocumentJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return objectMapper.readValue(processInstanceMigrationDocumentJson, ProcessInstanceMigrationDocumentImpl.class);
        } catch (IOException e) {
            throw new FlowableIllegalArgumentException("Low level I/O problem with Json argument", e);
        }
    }

    public void setMigrateToProcessDefinitionId(String processDefinitionId) {
        this.migrateToProcessDefinitionId = processDefinitionId;
    }

    @Override
    public String getMigrateToProcessDefinitionId() {
        return migrateToProcessDefinitionId;
    }

    public void setMigrateToProcessDefinition(String processDefinitionKey, String processDefinitionVersion) {
        this.migrateToProcessDefinitionKey = processDefinitionKey;
        this.migrateToProcessDefinitionVersion = processDefinitionVersion;
    }

    public void setMigrateToProcessDefinition(String processDefinitionKey, String processDefinitionVersion, String processDefinitionTenantId) {
        this.migrateToProcessDefinitionKey = processDefinitionKey;
        this.migrateToProcessDefinitionVersion = processDefinitionVersion;
        this.migrateToProcessDefinitionTenantId = processDefinitionTenantId;
    }

    @Override
    public String getMigrateToProcessDefinitionKey() {
        return migrateToProcessDefinitionKey;
    }

    @Override
    public String getMigrateToProcessDefinitionVersion() {
        return migrateToProcessDefinitionVersion;
    }

    @Override
    public String getMigrateToProcessDefinitionTenantId() {
        return migrateToProcessDefinitionTenantId;
    }

    public void setProcessInstancesIdsToMigrate(List<String> processInstancesIds) {
        this.processInstancesIdsToMigrate = processInstancesIds;
    }

    @Override
    public List<String> getProcessInstancesIdsToMigrate() {
        return processInstancesIdsToMigrate;
    }

    public void setActivityMigrationMappings(Map<String, String> activityMigrationMappings) {
        this.activityMigrationMappings = activityMigrationMappings;
    }

    @Override
    public Map<String, String> getActivityMigrationMappings() {
        return activityMigrationMappings;
    }

    @Override
    public String asJsonString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            //Wrap as RuntimeException
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "ProcessInstanceMigrationDocumentImpl{" +
            "migrateToProcessDefinitionId='" + migrateToProcessDefinitionId + '\'' +
            ", migrateToProcessDefinitionKey='" + migrateToProcessDefinitionKey + '\'' +
            ", migrateToProcessDefinitionVersion='" + migrateToProcessDefinitionVersion + '\'' +
            ", migrateToProcessDefinitionTenantId='" + migrateToProcessDefinitionTenantId + '\'' +
            ", processInstancesIdsToMigrate=" + processInstancesIdsToMigrate +
            ", activityMigrationMappings=" + activityMigrationMappings +
            '}';
    }
}
