package org.flowable.engine.impl.calendar;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.runtime.ClockReader;

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
        nextRun = new CronExpression(duedateDescription, clockReader, timeZone).getTimeAfter(clockReader.getCurrentCalendar(timeZone));
      }

    } catch (Exception e) {
      throw new FlowableException("Failed to parse scheduler expression: " + duedateDescription, e);
    }

    return nextRun == null ? null : nextRun.getTime();
  }

}
