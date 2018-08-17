package org.flowable.engine.migration;

import java.util.Map;

public interface ProcessInstanceMigrationDocumentBuilder {

    ProcessInstanceMigrationDocumentBuilder setProcessDefinitionToMigrateTo(String processDefinitionId);

    ProcessInstanceMigrationDocumentBuilder setProcessDefinitionToMigrateTo(String processDefinitionKey, String processDefinitionVersion);

    ProcessInstanceMigrationDocumentBuilder setProcessDefinitionToMigrateTo(String processDefinitionKey, String processDefinitionVersion, String processDefinitionTenantId);

    ProcessInstanceMigrationDocumentBuilder setTenantOfProcessDefinitionToMigrateTo(String processDefinitionTenantId);

    ProcessInstanceMigrationDocumentBuilder addProcessInstanceIdToMigrate(String processInstanceId);

    ProcessInstanceMigrationDocumentBuilder addProcessInstancesIdsToMigrate(String... processInstancesIds);

    ProcessInstanceMigrationDocumentBuilder addActivityMigrationMappings(Map<String, String> activityMigrationMappings);

    ProcessInstanceMigrationDocumentBuilder addActivityMigrationMapping(String fromActivityId, String toActivityId);

    ProcessInstanceMigrationDocument build();

}
