package org.flowable.engine.migration;

import java.util.Map;

public interface ProcessInstanceMigrationBuilder {

    ProcessInstanceMigrationBuilder migrateToProcessDefinition(String processDefinitionId);

    ProcessInstanceMigrationBuilder migrateToProcessDefinition(String processDefinitionKey, String processDefinitionVersion);

    ProcessInstanceMigrationBuilder migrateToProcessDefinition(String processDefinitionKey, String processDefinitionVersion, String processDefinitionTenantId);

    ProcessInstanceMigrationBuilder withMigrateToProcessDefinitionTenantId(String processDefinitionTenantId);

    ProcessInstanceMigrationBuilder addProcessInstanceToMigrate(String processInstanceId);

    ProcessInstanceMigrationBuilder addProcessInstancesToMigrate(String... processInstanceId);

    ProcessInstanceMigrationBuilder addActivityMigrationMapping(String fromActivityId, String toActivityId);

    ProcessInstanceMigrationBuilder addActivityMigrationMappings(Map<String, String> activityMigrationMappings);

    ProcessInstanceMigrationDocument getProcessInstanceMigrationDocument();

    void migrate();

}
