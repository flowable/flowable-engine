package org.flowable.engine.migration;

import java.util.List;
import java.util.Map;

public interface ProcessInstanceMigrationDocument {

    String getMigrateToProcessDefinitionId();

    String getMigrateToProcessDefinitionKey();

    String getMigrateToProcessDefinitionVersion();

    String getMigrateToProcessDefinitionTenantId();

    List<String> getProcessInstancesIdsToMigrate();

    Map<String, String> getActivityMigrationMappings();

    String asJsonString();

}
