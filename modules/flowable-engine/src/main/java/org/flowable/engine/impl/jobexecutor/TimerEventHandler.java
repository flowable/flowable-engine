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

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class TimerEventHandler {

    public static final String PROPERTYNAME_TIMER_ACTIVITY_ID = "activityId";
    public static final String PROPERTYNAME_END_DATE_EXPRESSION = "timerEndDate";
    public static final String PROPERTYNAME_CALENDAR_NAME_EXPRESSION = "calendarName";

    public static String createConfiguration(String id, String endDate, String calendarName) {
        JsonObject cfgJson = new JsonObject();
        cfgJson.addProperty(PROPERTYNAME_TIMER_ACTIVITY_ID, id);
        if (endDate != null) {
            cfgJson.addProperty(PROPERTYNAME_END_DATE_EXPRESSION, endDate);
        }
        if (calendarName != null) {
            cfgJson.addProperty(PROPERTYNAME_CALENDAR_NAME_EXPRESSION, calendarName);
        }
        return cfgJson.toString();
    }

    public static String setActivityIdToConfiguration(String jobHandlerConfiguration, String activityId) {
        try {
            JsonObject cfgJson = new JsonParser().parse(jobHandlerConfiguration).getAsJsonObject();
            cfgJson.addProperty(PROPERTYNAME_TIMER_ACTIVITY_ID, activityId);
            return cfgJson.toString();
        } catch (JsonParseException ex) {
            return jobHandlerConfiguration;
        }
    }

    public static String getActivityIdFromConfiguration(String jobHandlerConfiguration) {
        try {
        	JsonObject cfgJson = new JsonParser().parse(jobHandlerConfiguration).getAsJsonObject();
            return cfgJson.get(PROPERTYNAME_TIMER_ACTIVITY_ID) != null ? cfgJson.get(PROPERTYNAME_TIMER_ACTIVITY_ID).getAsString() : null;
        } catch (JsonParseException ex) {
            return jobHandlerConfiguration;
        }
    }

    public static String getCalendarNameFromConfiguration(String jobHandlerConfiguration) {
        try {
        	JsonObject cfgJson = new JsonParser().parse(jobHandlerConfiguration).getAsJsonObject();
            return cfgJson.get(PROPERTYNAME_CALENDAR_NAME_EXPRESSION) != null ? cfgJson.get(PROPERTYNAME_CALENDAR_NAME_EXPRESSION).getAsString() : null;
        } catch (JsonParseException ex) {
            // calendar name is not specified
            return "";
        }
    }

    public static String setEndDateToConfiguration(String jobHandlerConfiguration, String endDate) {
        JsonObject cfgJson = null;
        try {
            cfgJson = new JsonParser().parse(jobHandlerConfiguration).getAsJsonObject();
        } catch (JsonParseException ex) {
            // create the json config
            cfgJson = new JsonObject();
            cfgJson.addProperty(PROPERTYNAME_TIMER_ACTIVITY_ID, jobHandlerConfiguration);
        }
        if (endDate != null) {
            cfgJson.addProperty(PROPERTYNAME_END_DATE_EXPRESSION, endDate);
        }

        return cfgJson.toString();
    }

    public static String getEndDateFromConfiguration(String jobHandlerConfiguration) {
        try {
        	JsonObject cfgJson = new JsonParser().parse(jobHandlerConfiguration).getAsJsonObject();
            return cfgJson.get(PROPERTYNAME_END_DATE_EXPRESSION) != null ? cfgJson.get(PROPERTYNAME_END_DATE_EXPRESSION).getAsString() : null;
        } catch (JsonParseException ex) {
            return null;
        }
    }

}
