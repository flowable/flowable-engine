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
package org.flowable.spring;

import org.flowable.common.engine.impl.calendar.AdvancedCycleBusinessCalendar;
import org.flowable.common.engine.impl.calendar.BusinessCalendarManager;
import org.flowable.common.engine.impl.calendar.DueDateBusinessCalendar;
import org.flowable.common.engine.impl.calendar.DurationBusinessCalendar;
import org.flowable.common.engine.impl.calendar.MapBusinessCalendarManager;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.common.engine.impl.util.DefaultClockImpl;

/**
 * Creates an advanced cycle business calendar manager (ACBCM). The ACBCM can handle daylight savings changes when the scheduled time zone is different than the server time zone.
 * <p>
 * Create a factory bean
 * 
 * <pre>
 * &lt;bean id="businessCalendarManagerFactory" class="org.flowable.spring.SpringAdvancedBusinessCalendarManagerFactory" /&gt;
 * </pre>
 * 
 * Add the manager to your org.flowable.spring.SpringProcessEngineConfiguration bean
 * 
 * <pre>
 *  &lt;bean id="processEngineConfiguration" class="org.flowable.spring.SpringProcessEngineConfiguration"&gt;
 *    ...
 *    &lt;property name="businessCalendarManager"&gt;
 *      &lt;bean id="advancedBusinessCalendarManager" factory-bean="businessCalendarManagerFactory" factory-method="getBusinessCalendarManager" /&gt;
 *    &lt;/property&gt;
 *    ...
 *  &lt;/bean&gt;
 * </pre>
 * 
 * @author mseiden
 * @see AdvancedCycleBusinessCalendar
 */
public class SpringAdvancedBusinessCalendarManagerFactory {

    private Integer defaultScheduleVersion;

    private Clock clock;

    public Integer getDefaultScheduleVersion() {
        return defaultScheduleVersion;
    }

    public void setDefaultScheduleVersion(Integer defaultScheduleVersion) {
        this.defaultScheduleVersion = defaultScheduleVersion;
    }

    public Clock getClock() {
        if (clock == null) {
            clock = new DefaultClockImpl();
        }
        return clock;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public BusinessCalendarManager getBusinessCalendarManager() {
        MapBusinessCalendarManager mapBusinessCalendarManager = new MapBusinessCalendarManager();
        mapBusinessCalendarManager.addBusinessCalendar(DurationBusinessCalendar.NAME, new DurationBusinessCalendar(getClock()));
        mapBusinessCalendarManager.addBusinessCalendar(DueDateBusinessCalendar.NAME, new DueDateBusinessCalendar(getClock()));
        mapBusinessCalendarManager.addBusinessCalendar(AdvancedCycleBusinessCalendar.NAME, new AdvancedCycleBusinessCalendar(getClock(), defaultScheduleVersion));

        return mapBusinessCalendarManager;
    }

}
