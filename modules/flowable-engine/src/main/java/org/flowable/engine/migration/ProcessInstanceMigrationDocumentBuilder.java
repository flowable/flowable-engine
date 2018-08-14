package org.flowable.engine.migration;

import java.util.Map;

public interface ProcessInstanceMigrationDocumentBuilder {

//    ProcessInstanceMigrationDocumentBuilder toProcessDefinitionId(String processDefinitionId);
//
//    ProcessInstanceMigrationDocumentBuilder toProcessDefinitionVersion(String processDefinitionKey, String processDefinitionVersion);

    ProcessInstanceMigrationDocumentBuilder addProcessInstanceIdToMigrate(String processInstanceId);

    ProcessInstanceMigrationDocumentBuilder addProcessInstancesIdsToMigrate(String... processInstancesIds);

    ProcessInstanceMigrationDocumentBuilder addActivityMigrationMappings(Map<String, String> activityMigrationMappings);

    ProcessInstanceMigrationDocumentBuilder addActivityMigrationMapping(String fromActivityId, String toActivityId);

    ProcessInstanceMigrationDocument build();

}
