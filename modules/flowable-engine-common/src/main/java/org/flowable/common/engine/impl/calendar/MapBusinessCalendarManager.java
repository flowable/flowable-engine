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
package org.flowable.common.engine.impl.calendar;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.common.engine.impl.calendar.BusinessCalendarManager;

/**
 * @author Tom Baeyens
 */
public class MapBusinessCalendarManager implements BusinessCalendarManager {

    private final Map<String, BusinessCalendar> businessCalendars;

    public MapBusinessCalendarManager() {
        this.businessCalendars = new HashMap<>();
    }

    public MapBusinessCalendarManager(Map<String, BusinessCalendar> businessCalendars) {
        if (businessCalendars == null) {
            throw new IllegalArgumentException("businessCalendars can not be null");
        }

        this.businessCalendars = new HashMap<>(businessCalendars);
    }

    @Override
    public BusinessCalendar getBusinessCalendar(String businessCalendarRef) {
        BusinessCalendar businessCalendar = businessCalendars.get(businessCalendarRef);
        if (businessCalendar == null) {
            throw new FlowableException("Requested business calendar " + businessCalendarRef +
                    " does not exist. Allowed calendars are " + this.businessCalendars.keySet() + ".");
        }
        return businessCalendar;
    }

    public BusinessCalendarManager addBusinessCalendar(String businessCalendarRef, BusinessCalendar businessCalendar) {
        businessCalendars.put(businessCalendarRef, businessCalendar);
        return this;
    }
}
