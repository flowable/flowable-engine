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
package org.flowable.cmmn.engine.impl.migration;

import java.io.IOException;

import org.flowable.cmmn.api.migration.HistoricCaseInstanceMigrationDocument;
import org.flowable.common.engine.api.FlowableException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class HistoricCaseInstanceMigrationDocumentConverter implements CaseInstanceMigrationDocumentConstants {

    protected static ObjectMapper objectMapper = new ObjectMapper();
    
    public static JsonNode convertToJson(HistoricCaseInstanceMigrationDocument historicCaseInstanceMigrationDocument) {
        ObjectNode documentNode = objectMapper.createObjectNode();

        if (historicCaseInstanceMigrationDocument.getMigrateToCaseDefinitionId() != null) {
            documentNode.put(TO_CASE_DEFINITION_ID_JSON_PROPERTY, historicCaseInstanceMigrationDocument.getMigrateToCaseDefinitionId());
        }

        if (historicCaseInstanceMigrationDocument.getMigrateToCaseDefinitionKey() != null) {
            documentNode.put(TO_CASE_DEFINITION_KEY_JSON_PROPERTY, historicCaseInstanceMigrationDocument.getMigrateToCaseDefinitionKey());
        }

        if (historicCaseInstanceMigrationDocument.getMigrateToCaseDefinitionVersion() != null) {
            documentNode.put(TO_CASE_DEFINITION_VERSION_JSON_PROPERTY, historicCaseInstanceMigrationDocument.getMigrateToCaseDefinitionVersion());
        }

        if (historicCaseInstanceMigrationDocument.getMigrateToCaseDefinitionTenantId() != null) {
            documentNode.put(TO_CASE_DEFINITION_TENANT_ID_JSON_PROPERTY, historicCaseInstanceMigrationDocument.getMigrateToCaseDefinitionTenantId());
        }

        return documentNode;
    }
    
    public static String convertToJsonString(HistoricCaseInstanceMigrationDocument historicCaseInstanceMigrationDocument) {
        JsonNode jsonNode = convertToJson(historicCaseInstanceMigrationDocument);
        ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
        try {
            return objectWriter.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            return jsonNode.toString();
        }
    }

    public static HistoricCaseInstanceMigrationDocument convertFromJson(String jsonCaseInstanceMigrationDocument) {

        try {
            JsonNode rootNode = objectMapper.readTree(jsonCaseInstanceMigrationDocument);
            HistoricCaseInstanceMigrationDocumentBuilderImpl documentBuilder = new HistoricCaseInstanceMigrationDocumentBuilderImpl();

            documentBuilder.setCaseDefinitionToMigrateTo(getJsonProperty(TO_CASE_DEFINITION_ID_JSON_PROPERTY, rootNode));
            
            String caseDefinitionKey = getJsonProperty(TO_CASE_DEFINITION_KEY_JSON_PROPERTY, rootNode);
            Integer caseDefinitionVersion = getJsonPropertyAsInteger(TO_CASE_DEFINITION_VERSION_JSON_PROPERTY, rootNode);
            documentBuilder.setCaseDefinitionToMigrateTo(caseDefinitionKey, caseDefinitionVersion);

            documentBuilder.setTenantId(getJsonProperty(TO_CASE_DEFINITION_TENANT_ID_JSON_PROPERTY, rootNode));

            return documentBuilder.build();

        } catch (IOException e) {
            throw new FlowableException("Error parsing Historic Case Instance Migration Document", e);
        }

    }
    
    protected static String getJsonProperty(String propertyName, JsonNode jsonNode) {
        if (jsonNode.has(propertyName) && !jsonNode.get(propertyName).isNull()) {
            return jsonNode.get(propertyName).asText();
        }
        
        return null;
    }
    
    protected static Integer getJsonPropertyAsInteger(String propertyName, JsonNode jsonNode) {
        if (jsonNode.has(propertyName) && !jsonNode.get(propertyName).isNull()) {
            return jsonNode.get(propertyName).asInt();
        }
        
        return null;
    }
}

