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
package org.flowable.engine.impl.jobexecutor;

import java.io.IOException;

import org.flowable.engine.impl.util.CommandContextUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TimerEventHandler {

    public static final String PROPERTYNAME_TIMER_ACTIVITY_ID = "activityId";
    public static final String PROPERTYNAME_END_DATE_EXPRESSION = "timerEndDate";
    public static final String PROPERTYNAME_CALENDAR_NAME_EXPRESSION = "calendarName";

    public static String createConfiguration(String id, String endDate, String calendarName) {
        ObjectNode cfgJson = createObjectNode();
        cfgJson.put(PROPERTYNAME_TIMER_ACTIVITY_ID, id);
        if (endDate != null) {
            cfgJson.put(PROPERTYNAME_END_DATE_EXPRESSION, endDate);
        }
        if (calendarName != null) {
            cfgJson.put(PROPERTYNAME_CALENDAR_NAME_EXPRESSION, calendarName);
        }
        return cfgJson.toString();
    }

    public static String setActivityIdToConfiguration(String jobHandlerConfiguration, String activityId) {
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

    public static String getCalendarNameFromConfiguration(String jobHandlerConfiguration) {
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

    public static String setEndDateToConfiguration(String jobHandlerConfiguration, String endDate) {
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
    
    protected static ObjectNode createObjectNode() {
        return CommandContextUtil.getProcessEngineConfiguration().getObjectMapper().createObjectNode();
    }
    
    protected static ObjectNode readJsonValueAsObjectNode(String config) throws IOException {
        return (ObjectNode) readJsonValue(config);
    }
    
    protected static JsonNode readJsonValue(String config) throws IOException {
        if (CommandContextUtil.getCommandContext() != null) {
            return CommandContextUtil.getProcessEngineConfiguration().getObjectMapper().readTree(config);
        } else {
            return new ObjectMapper().readTree(config);
        }
    }

}
