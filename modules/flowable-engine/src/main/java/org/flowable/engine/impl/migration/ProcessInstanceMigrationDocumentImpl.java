package org.flowable.engine.impl.migration;

import java.util.List;
import java.util.Map;

import org.flowable.engine.migration.ProcessInstanceMigrationDocument;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProcessInstanceMigrationDocumentImpl implements ProcessInstanceMigrationDocument {

    protected String migrateToProcessDefinitionId;
    protected String migrateToProcessDefinitionKey;
    protected String migrateToProcessDefinitionVersion;
    protected List<String> processInstancesIdsToMigrate;
    protected Map<String, String> activityMigrationMappings;

    public void setMigrateToProcessDefinitionId(String processDefinitionId) {
        this.migrateToProcessDefinitionId = processDefinitionId;
    }

    @Override
    public String getMigrateToProcessDefinitionId() {
        return migrateToProcessDefinitionId;
    }

    public void setMigrateToProcessDefinitionKey(String processDefinitionKey) {
        this.migrateToProcessDefinitionKey = processDefinitionKey;
    }

    @Override
    public String getMigrateToProcessDefinitionKey() {
        return migrateToProcessDefinitionKey;
    }

    public void setMigrateToProcessDefinitionVersion(String processDefinitionVersion) {
        this.migrateToProcessDefinitionVersion = processDefinitionVersion;
    }

    @Override
    public String getMigrateToProcessDefinitionVersion() {
        return migrateToProcessDefinitionVersion;
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

    //TODO keep the json representation cached in a String and prepared from the Builder instead!!?
    //TODO use toString() instead returning a Json document?
    @Override
    public String toJsonString() {
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
            ", processInstancesIdsToMigrate=" + processInstancesIdsToMigrate +
            ", activityMigrationMappings=" + activityMigrationMappings +
            '}';
    }
}
