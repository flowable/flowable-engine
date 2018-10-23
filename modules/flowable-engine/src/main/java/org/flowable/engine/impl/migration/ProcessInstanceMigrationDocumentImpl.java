/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine.impl.migration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.migration.ActivityMigrationMapping;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationDocumentConverter;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationDocumentImpl implements ProcessInstanceMigrationDocument {

    protected String migrateToProcessDefinitionId;
    protected String migrateToProcessDefinitionKey;
    protected Integer migrateToProcessDefinitionVersion;
    protected String migrateToProcessDefinitionTenantId;
    protected List<ActivityMigrationMapping> activityMigrationMappings;
    protected Map<String, Map<String, Object>> activitiesLocalVariables;
    protected Map<String, Object> processInstanceVariables;
    protected Set<String> mappedFromActivities;

    public static ProcessInstanceMigrationDocument fromProcessInstanceMigrationDocumentJson(String processInstanceMigrationDocumentJson) {
        return ProcessInstanceMigrationDocumentConverter.convertFromJson(processInstanceMigrationDocumentJson);
    }

    public void setMigrateToProcessDefinitionId(String processDefinitionId) {
        this.migrateToProcessDefinitionId = processDefinitionId;
    }

    public void setMigrateToProcessDefinition(String processDefinitionKey, Integer processDefinitionVersion) {
        this.migrateToProcessDefinitionKey = processDefinitionKey;
        this.migrateToProcessDefinitionVersion = processDefinitionVersion;
    }

    public void setMigrateToProcessDefinition(String processDefinitionKey, Integer processDefinitionVersion, String processDefinitionTenantId) {
        this.migrateToProcessDefinitionKey = processDefinitionKey;
        this.migrateToProcessDefinitionVersion = processDefinitionVersion;
        this.migrateToProcessDefinitionTenantId = processDefinitionTenantId;
    }

    @Override
    public String getMigrateToProcessDefinitionId() {
        return migrateToProcessDefinitionId;
    }

    @Override
    public String getMigrateToProcessDefinitionKey() {
        return migrateToProcessDefinitionKey;
    }

    @Override
    public Integer getMigrateToProcessDefinitionVersion() {
        return migrateToProcessDefinitionVersion;
    }

    @Override
    public String getMigrateToProcessDefinitionTenantId() {
        return migrateToProcessDefinitionTenantId;
    }

    public void setActivityMigrationMappings(List<ActivityMigrationMapping> activityMigrationMappings) {
        List<String> duplicates = findDuplicatedFromActivityIds(activityMigrationMappings);
        if (duplicates.isEmpty()) {
            this.activityMigrationMappings = activityMigrationMappings;
            this.activitiesLocalVariables = buildActivitiesLocalVariablesMap(activityMigrationMappings);
            this.mappedFromActivities = extractMappedFromActivities(activityMigrationMappings);
        } else {
            throw new FlowableException("From activity '" + Arrays.toString(duplicates.toArray()) + "' is mapped more than once");
        }
    }

    protected static List<String> findDuplicatedFromActivityIds(List<ActivityMigrationMapping> activityMigrationMappings) {
        //Frequency Map
        Map<String, Long> frequencyMap = activityMigrationMappings.stream()
            .flatMap(mapping -> mapping.getFromActivityIds().stream())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        //Duplicates
        List<String> duplicatedActivityIds = frequencyMap.entrySet()
            .stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        return duplicatedActivityIds;
    }

    protected static Map<String, Map<String, Object>> buildActivitiesLocalVariablesMap(List<ActivityMigrationMapping> activityMigrationMappings) {

        Map<String, Map<String, Object>> variablesMap = new HashMap<>();
        activityMigrationMappings.forEach(mapping -> {
            mapping.getToActivityIds().forEach(activityId -> {
                Map<String, Object> mappedLocalVariables = null;
                if (mapping instanceof ActivityMigrationMapping.OneToOneMapping) {
                    mappedLocalVariables = ((ActivityMigrationMapping.OneToOneMapping) mapping).getActivityLocalVariables();
                }
                if (mapping instanceof ActivityMigrationMapping.ManyToOneMapping) {
                    mappedLocalVariables = ((ActivityMigrationMapping.ManyToOneMapping) mapping).getActivityLocalVariables();
                }
                if (mapping instanceof ActivityMigrationMapping.OneToManyMapping) {
                    mappedLocalVariables = ((ActivityMigrationMapping.OneToManyMapping) mapping).getActivitiesLocalVariables().get(activityId);
                }
                if (mappedLocalVariables != null && !mappedLocalVariables.isEmpty()) {
                    Map<String, Object> activityLocalVariables = variablesMap.computeIfAbsent(activityId, key -> new HashMap<>());
                    activityLocalVariables.putAll(mappedLocalVariables);
                }
            });
        });
        return variablesMap;
    }

    protected static Set<String> extractMappedFromActivities(List<ActivityMigrationMapping> activityMigrationMappings) {
        Set<String> fromActivities = activityMigrationMappings.stream()
            .flatMap(mapping -> mapping.getFromActivityIds().stream())
            .collect(Collectors.toSet());
        return fromActivities;
    }

    @Override
    public List<ActivityMigrationMapping> getActivityMigrationMappings() {
        return activityMigrationMappings;
    }

    @Override
    public Map<String, Map<String, Object>> getActivitiesLocalVariables() {
        return activitiesLocalVariables;
    }

    public void setProcessInstanceVariables(Map<String, Object> processInstanceVariables) {
        this.processInstanceVariables = processInstanceVariables;
    }

    @Override
    public Map<String, Object> getProcessInstanceVariables() {
        return processInstanceVariables;
    }

    @Override
    public String asJsonString() {
        JsonNode jsonNode = ProcessInstanceMigrationDocumentConverter.convertToJson(this);
        return jsonNode.toString();
    }

    @Override
    public String toString() {
        return "ProcessInstanceMigrationDocumentImpl{" +
            "migrateToProcessDefinitionId='" + migrateToProcessDefinitionId + '\'' +
            ", migrateToProcessDefinitionKey='" + migrateToProcessDefinitionKey + '\'' +
            ", migrateToProcessDefinitionVersion='" + migrateToProcessDefinitionVersion + '\'' +
            ", migrateToProcessDefinitionTenantId='" + migrateToProcessDefinitionTenantId + '\'' +
            ", activityMigrationMappings=" + activityMigrationMappings +
            '}';
    }
}
