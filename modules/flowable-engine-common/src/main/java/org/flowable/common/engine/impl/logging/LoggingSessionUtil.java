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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LoggingSessionUtil {
    
    public static final String ID = "__id";
    public static final String TRANSACTION_ID = "__transactionId";
    public static final String TIMESTAMP = "__timeStamp";
    public static final String LOG_NUMBER = "__logNumber";
    
    protected static TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
    protected static StrongUuidGenerator idGenerator = new StrongUuidGenerator();
    
    public static void addLoggingData(String type, String message, String scopeId, String subScopeId, String scopeType, 
            String scopeDefinitionId, String elementId, String elementName, String elementType, String elementSubType, 
            String engineType, ObjectMapper objectMapper) {
        
        ObjectNode loggingNode = fillLoggingData(message, scopeId, subScopeId, scopeType, scopeDefinitionId, elementId, 
                elementName, elementType, elementSubType, objectMapper);
        addLoggingData(type, loggingNode, engineType);
    }
    
    public static void addErrorLoggingData(String type, ObjectNode loggingNode, Throwable t, String engineType) {
        ObjectNode exceptionNode = loggingNode.putObject("exception");
        exceptionNode.put("message", t.getMessage());
        exceptionNode.put("stackTrace", ExceptionUtils.getStackTrace(t));
        addLoggingData(type, loggingNode, engineType);
    }
    
    public static void addLoggingData(String type, String message, String engineType, ObjectMapper objectMapper) {
        ObjectNode loggingNode = objectMapper.createObjectNode();
        loggingNode.put("message", message);
        addLoggingData(type, loggingNode, engineType);
    }
    
    public static void addEngineLoggingData(String type, String message, String engineType, ObjectMapper objectMapper) {
        ObjectNode loggingNode = objectMapper.createObjectNode();
        loggingNode.put("message", message);
        loggingNode.put("engineType", engineType);
        
        LoggingSession loggingSession = Context.getCommandContext().getSession(LoggingSession.class);
        List<ObjectNode> loggingData = loggingSession.getLoggingData();
        if (loggingData != null) {
            for (ObjectNode itemNode : loggingData) {
                if (itemNode.has("scopeId") && itemNode.has("scopeDefinitionKey")) {
                    loggingNode.put("scopeId", itemNode.get("scopeId").asText());
                    loggingNode.put("scopeType", itemNode.get("scopeType").asText());
                    loggingNode.put("scopeDefinitionId", itemNode.get("scopeDefinitionId").asText());
                    loggingNode.put("scopeDefinitionKey", itemNode.get("scopeDefinitionKey").asText());
                    if (itemNode.has("scopeDefinitionName") && !itemNode.get("scopeDefinitionName").isNull()) {
                        loggingNode.put("scopeDefinitionName", itemNode.get("scopeDefinitionName").asText());
                    }
                }
            }
        }
        
        addLoggingData(type, loggingNode, engineType);
    }
    
    public static void addLoggingData(String type, ObjectNode data, String engineType) {
        data.put(ID, idGenerator.getNextId());
        data.put(TIMESTAMP, formatDate(new Date()));
        data.put("type", type);
        
        LoggingSession loggingSession = Context.getCommandContext().getSession(LoggingSession.class);
        loggingSession.addLoggingData(type, data, engineType);
    }
    
    public static ObjectNode fillLoggingData(String message, String scopeId, String subScopeId, String scopeType, 
            String scopeDefinitionId, String elementId, String elementName, String elementType, String elementSubType, ObjectMapper objectMapper) {
        
        ObjectNode loggingNode = fillLoggingData(message, scopeId, subScopeId, scopeType, objectMapper);
        loggingNode.put("scopeDefinitionId", scopeDefinitionId);
        
        if (StringUtils.isNotEmpty(elementId)) {
            loggingNode.put("elementId", elementId);
        }
        
        if (StringUtils.isNotEmpty(elementName)) {
            loggingNode.put("elementName", elementName);
        }
        
        if (StringUtils.isNotEmpty(elementType)) {
            loggingNode.put("elementType", elementType);
        }
        
        if (StringUtils.isNotEmpty(elementSubType)) {
            loggingNode.put("elementSubType", elementSubType);
        }
        
        return loggingNode;
    }
    
    public static ObjectNode fillLoggingData(String message, String scopeId, String subScopeId, String scopeType, ObjectMapper objectMapper) {
        ObjectNode loggingNode = objectMapper.createObjectNode();
        loggingNode.put("message", message);
        loggingNode.put("scopeId", scopeId);
        
        if (StringUtils.isNotEmpty(subScopeId)) {
            loggingNode.put("subScopeId", subScopeId);
        }
        
        loggingNode.put("scopeType", scopeType);
        
        return loggingNode;
    }

    public static String formatDate(Date date) {
        if (date != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            dateFormat.setTimeZone(utcTimeZone);
            return dateFormat.format(date);
        }
        return null;
    }
    
    public static String formatDate(DateTime date) {
        if (date != null) {
            return date.toString("yyyy-MM-dd'T'hh:mm:ss.sss'Z'");
        }
        return null;
    }
    
    public static String formatDate(LocalDate date) {
        if (date != null) {
            return date.toString("yyyy-MM-dd");
        }
        return null;
    }
}
