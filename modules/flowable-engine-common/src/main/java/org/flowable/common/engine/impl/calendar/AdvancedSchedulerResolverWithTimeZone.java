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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.runtime.ClockReader;

/**
 * Resolves a due date taking into account the specified time zone.
 * 
 * @author mseiden
 */
public class AdvancedSchedulerResolverWithTimeZone implements AdvancedSchedulerResolver {

    @Override
    public Date resolve(String duedateDescription, ClockReader clockReader, TimeZone timeZone) {
        Calendar nextRun = null;

        try {
            if (duedateDescription.startsWith("R")) {
                nextRun = new DurationHelper(duedateDescription, clockReader).getCalendarAfter(clockReader.getCurrentCalendar(timeZone));
            } else {
                CronExpression cronExpression = new CronExpression(duedateDescription, clockReader);
                cronExpression.setTimeZone(timeZone);
                Date nextRunDate = cronExpression.getTimeAfter(clockReader.getCurrentCalendar(timeZone).getTime());
                nextRun = new GregorianCalendar();
                nextRun.setTime(nextRunDate);
            }

        } catch (Exception e) {
            throw new FlowableException("Failed to parse scheduler expression: " + duedateDescription, e);
        }

        return nextRun == null ? null : nextRun.getTime();
    }

}
