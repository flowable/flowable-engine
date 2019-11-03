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
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LoggingSessionUtil {
    
    public static final String TIMESTAMP = "__timeStamp";
    public static final String LOG_NUMBER = "__logNumber";
    
    protected static TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
    
    public static void addLoggingData(String type, String message, String scopeId, String subScopeId, String scopeType, 
                    String scopeDefinitionId, String elementId, String elementName, String elementType, String elementSubType) {
        
        ObjectNode loggingNode = fillLoggingData(message, scopeId, subScopeId, scopeType, scopeDefinitionId, elementId, elementName, elementType, elementSubType);
        addLoggingData(type, loggingNode);
    }
    
    public static void addErrorLoggingData(String type, ObjectNode loggingNode, Throwable t) {
        ObjectNode exceptionNode = loggingNode.putObject("exception");
        exceptionNode.put("message", t.getMessage());
        exceptionNode.put("stackTrace", ExceptionUtils.getStackTrace(t));
        addLoggingData(type, loggingNode);
    }
    
    public static void addLoggingData(String type, String message) {
        ObjectNode loggingNode = getObjectMapper().createObjectNode();
        loggingNode.put("message", message);
        addLoggingData(type, loggingNode);
    }
    
    public static void addEngineLoggingData(String type, String message, String engineType) {
        ObjectNode loggingNode = getObjectMapper().createObjectNode();
        loggingNode.put("message", message);
        loggingNode.put("engineType", engineType);
        addLoggingData(type, loggingNode);
    }
    
    public static void addLoggingData(String type, ObjectNode data) {
        data.put(TIMESTAMP, formatDate(new Date()));
        data.put("type", type);
        
        LoggingSession loggingSession = Context.getCommandContext().getSession(LoggingSession.class);
        loggingSession.addLoggingData(type, data);
    }
    
    public static ObjectNode fillLoggingData(String message, String scopeId, String subScopeId, String scopeType, 
                    String scopeDefinitionId, String elementId, String elementName, String elementType, String elementSubType) {
        
        ObjectNode loggingNode = fillLoggingData(message, scopeId, subScopeId, scopeType);
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
    
    public static ObjectNode fillLoggingData(String message, String scopeId, String subScopeId, String scopeType) {
        ObjectNode loggingNode = getObjectMapper().createObjectNode();
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
    
    protected static String getEngineType(CommandContext commandContext) {
        String engineName = null;
        if (commandContext.getCurrentEngineConfiguration().getEngineName() != null) {
            String engineConfigKey = commandContext.getCurrentEngineConfiguration().getEngineCfgKey();
            if (EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG.equals(engineConfigKey)) {
                engineName = "bpmn";
            } else if (EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG.equals(engineConfigKey)) {
                engineName = "cmmn";
            } else if (EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG.equals(engineConfigKey)) {
                engineName = "dmn";
            } else if (EngineConfigurationConstants.KEY_APP_ENGINE_CONFIG.equals(engineConfigKey)) {
                engineName = "app";
            } else if (EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG.equals(engineConfigKey)) {
                engineName = "form";
            } else if (EngineConfigurationConstants.KEY_CONTENT_ENGINE_CONFIG.equals(engineConfigKey)) {
                engineName = "content";
            } else if (EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG.equals(engineConfigKey)) {
                engineName = "idm";
            }
        }
        
        return engineName;
    }
    
    protected static ObjectMapper getObjectMapper() {
        return getEngineConfiguration().getObjectMapper();
    }
    
    protected static AbstractEngineConfiguration getEngineConfiguration() {
        return Context.getCommandContext().getCurrentEngineConfiguration();
    }
}
