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
package org.flowable.common.engine.impl.logging;

import java.util.List;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class LoggingSessionLoggerOutput {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSessionLoggerOutput.class);
    
    public static void printLogNodes(List<ObjectNode> logNodes) {
        printLogNodes(logNodes, "info");
    }
    
    public static void printLogNodes(List<ObjectNode> logNodes, String logLevel) {
        if (logNodes == null) {
            return;
        }
        
        StringBuilder logBuilder = new StringBuilder("\n");
        for (ObjectNode logNode : logNodes) {
            logBuilder.append(logNode.get(LoggingSessionUtil.TIMESTAMP).asText()).append(": ");
            
            String scopeType = null;
            if (logNode.has("scopeType")) {
                scopeType = logNode.get("scopeType").asText();
                if (ScopeTypes.BPMN.equals(scopeType)) {
                    logBuilder.append("(").append(logNode.get("scopeId").asText());
                    if (logNode.has("subScopeId")) {
                        logBuilder.append(",").append(logNode.get("subScopeId").asText());
                    }
                    logBuilder.append(") ");
                }
            }
            
            logBuilder.append(logNode.get("message").asText());
            
            if (ScopeTypes.BPMN.equals(scopeType)) {
                logBuilder.append(" (processInstanceId: '").append(logNode.get("scopeId").asText());
                if (logNode.has("subScopeId")) {
                    logBuilder.append("', executionId: '").append(logNode.get("subScopeId").asText());
                }
                
                logBuilder.append("', processDefinitionId: '").append(logNode.get("scopeDefinitionId").asText()).append("'");
            }
            
            if (logNode.has("elementId")) {
                logBuilder.append(", elementId: '").append(logNode.get("elementId").asText());
                if (logNode.has("elementName")) {
                    logBuilder.append("', elementName: '").append(logNode.get("elementName").asText());
                }
                
                logBuilder.append("', elementType: '").append(logNode.get("elementType").asText()).append("'");
            }
            
            logBuilder.append(")\n");
        }
        
        log(logBuilder.toString(), logLevel);
    }
    
    protected static void log(String message, String logLevel) {
        if ("info".equalsIgnoreCase(logLevel)) {
            LOGGER.info(message);
        } else if ("error".equalsIgnoreCase(logLevel)) {
            LOGGER.error(message);
        } else if ("warn".equalsIgnoreCase(logLevel)) {
            LOGGER.warn(message);
        } else if ("debug".equalsIgnoreCase(logLevel)) {
            LOGGER.debug(message);
        } else if ("trace".equalsIgnoreCase(logLevel)) {
            LOGGER.trace(message);
        } 
    }
}
