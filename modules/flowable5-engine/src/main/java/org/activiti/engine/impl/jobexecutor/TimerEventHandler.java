package org.activiti.engine.impl.jobexecutor;
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

import java.io.IOException;

import org.activiti.engine.impl.context.Context;
import org.flowable.common.engine.api.delegate.Expression;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TimerEventHandler {

    public static final String PROPERTYNAME_TIMER_ACTIVITY_ID = "activityId";
    public static final String PROPERTYNAME_END_DATE_EXPRESSION = "timerEndDate";
    public static final String PROPERTYNAME_PROCESS_DEFINITION_KEY = "processDefinitionKey";
    public static final String PROPERTYNAME_CALENDAR_NAME_EXPRESSION = "calendarName";

    public static String createConfiguration(String id, Expression endDate, Expression calendarName) {
        ObjectNode cfgJson = createObjectNode();
        cfgJson.put(PROPERTYNAME_TIMER_ACTIVITY_ID, id);
        if (endDate != null) {
            cfgJson.put(PROPERTYNAME_END_DATE_EXPRESSION, endDate.getExpressionText());
        }
        if (calendarName != null) {
            cfgJson.put(PROPERTYNAME_CALENDAR_NAME_EXPRESSION, calendarName.getExpressionText());
        }
        return cfgJson.toString();
    }

    public String setActivityIdToConfiguration(String jobHandlerConfiguration, String activityId) {
        try {
            ObjectNode cfgJson = readJsonValueAsObjectNode(jobHandlerConfiguration);
            cfgJson.put(PROPERTYNAME_TIMER_ACTIVITY_ID, activityId);
            return cfgJson.toString();
        } catch (IOException ex) {
            return jobHandlerConfiguration;
        }
    }

    public static String getActivityIdFromConfiguration(String jobHandlerConfiguration) {
        try {
            JsonNode cfgJson = readJsonValue(jobHandlerConfiguration);
            JsonNode activityIdNode = cfgJson.get(PROPERTYNAME_TIMER_ACTIVITY_ID);
            if (activityIdNode != null) {
                return activityIdNode.asText();
            } else {
                return jobHandlerConfiguration;
            }
            
        } catch (IOException ex) {
            return jobHandlerConfiguration;
        }
    }

    public static String geCalendarNameFromConfiguration(String jobHandlerConfiguration) {
        try {
            JsonNode cfgJson = readJsonValue(jobHandlerConfiguration);
            JsonNode calendarNameNode = cfgJson.get(PROPERTYNAME_CALENDAR_NAME_EXPRESSION);
            if (calendarNameNode != null) {
                return calendarNameNode.asText();
            } else {
                return "";
            }
            
        } catch (IOException ex) {
            // calendar name is not specified
            return "";
        }
    }

    public String setEndDateToConfiguration(String jobHandlerConfiguration, String endDate) {
        ObjectNode cfgJson = null;
        try {
            cfgJson = readJsonValueAsObjectNode(jobHandlerConfiguration);
        } catch (IOException ex) {
            // create the json config
            cfgJson = createObjectNode();
            cfgJson.put(PROPERTYNAME_TIMER_ACTIVITY_ID, jobHandlerConfiguration);
        }
        
        if (endDate != null) {
            cfgJson.put(PROPERTYNAME_END_DATE_EXPRESSION, endDate);
        }

        return cfgJson.toString();
    }

    public static String getEndDateFromConfiguration(String jobHandlerConfiguration) {
        try {
            JsonNode cfgJson = readJsonValue(jobHandlerConfiguration);
            JsonNode endDateNode = cfgJson.get(PROPERTYNAME_END_DATE_EXPRESSION);
            if (endDateNode != null) {
                return endDateNode.asText();
            } else {
                return null;
            }
            
        } catch (IOException ex) {
            return null;
        }
    }

    public String setProcessDefinitionKeyToConfiguration(String jobHandlerConfiguration, String processDefinitionKey) {
        ObjectNode cfgJson = null;
        try {
            cfgJson = readJsonValueAsObjectNode(jobHandlerConfiguration);
            cfgJson.put(PROPERTYNAME_PROCESS_DEFINITION_KEY, processDefinitionKey);
            return cfgJson.toString();
            
        } catch (IOException ex) {
            return jobHandlerConfiguration;
        }
    }

    public String getProcessDefinitionKeyFromConfiguration(String jobHandlerConfiguration) {
        try {
            JsonNode cfgJson = readJsonValue(jobHandlerConfiguration);
            JsonNode keyNode = cfgJson.get(PROPERTYNAME_PROCESS_DEFINITION_KEY);
            if (keyNode != null) {
                return keyNode.asText();
            } else {
                return null;
            }
            
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Before Activiti 5.21, the jobHandlerConfiguration would have as activityId the process definition key (as only one timer start event was supported). In >= 5.21, this changed and in >= 5.21 the
     * activityId is the REAL activity id. It can be recognized by having the 'processDefinitionKey' in the configuration. A < 5.21 job would not have that.
     */
    public static boolean hasRealActivityId(String jobHandlerConfiguration) {
        try {
            JsonNode cfgJson = readJsonValue(jobHandlerConfiguration);
            JsonNode keyNode = cfgJson.get(PROPERTYNAME_PROCESS_DEFINITION_KEY);
            if (keyNode != null) {
                return keyNode.asText().length() > 0;
            } else {
                return false;
            }
            
        } catch (IOException ex) {
            return false;
        }
    }

    protected static ObjectNode createObjectNode() {
        return Context.getProcessEngineConfiguration().getObjectMapper().createObjectNode();
    }
    
    protected static ObjectNode readJsonValueAsObjectNode(String config) throws IOException {
        return (ObjectNode) readJsonValue(config);
    }
    
    protected static JsonNode readJsonValue(String config) throws IOException {
        if (Context.getCommandContext() != null) {
            return Context.getProcessEngineConfiguration().getObjectMapper().readTree(config);
        } else {
            return new ObjectMapper().readTree(config);
        }
    }
}
