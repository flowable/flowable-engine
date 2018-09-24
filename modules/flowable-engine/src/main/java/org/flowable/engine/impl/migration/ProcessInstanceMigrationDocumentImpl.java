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

import java.io.IOException;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationDocumentImpl implements ProcessInstanceMigrationDocument {

    protected String migrateToProcessDefinitionId;
    protected String migrateToProcessDefinitionKey;
    protected int migrateToProcessDefinitionVersion;
    protected String migrateToProcessDefinitionTenantId;
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

    public void setMigrateToProcessDefinition(String processDefinitionKey, int processDefinitionVersion) {
        this.migrateToProcessDefinitionKey = processDefinitionKey;
        this.migrateToProcessDefinitionVersion = processDefinitionVersion;
    }

    public void setMigrateToProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId) {
        this.migrateToProcessDefinitionKey = processDefinitionKey;
        this.migrateToProcessDefinitionVersion = processDefinitionVersion;
        this.migrateToProcessDefinitionTenantId = processDefinitionTenantId;
    }

    @Override
    public String getMigrateToProcessDefinitionKey() {
        return migrateToProcessDefinitionKey;
    }

    @Override
    public int getMigrateToProcessDefinitionVersion() {
        return migrateToProcessDefinitionVersion;
    }

    @Override
    public String getMigrateToProcessDefinitionTenantId() {
        return migrateToProcessDefinitionTenantId;
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
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
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
            ", activityMigrationMappings=" + activityMigrationMappings +
            '}';
    }
}
