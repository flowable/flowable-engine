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
package org.flowable.common.engine.impl.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author Filip Hrisafov
 */
public class DateUtil {

    /**
     * This is the closest date formatter we can get to what used to be supported with Joda DateTime.
     * This makes everything (except the year) optional.
     */
    private static final DateTimeFormatter defaultDateFormatter =
            // @formatter:off
            new DateTimeFormatterBuilder()
                    .appendValue(ChronoField.YEAR, 1, 10, SignStyle.NORMAL)
                    .optionalStart()
                        .appendLiteral('-')
                        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                    .optionalEnd()
                    .optionalStart()
                        .appendLiteral('-')
                        .appendValue(ChronoField.DAY_OF_MONTH, 2)
                    .optionalEnd()
                    .optionalStart()
                        .appendLiteral('T')
                        .appendValue(ChronoField.HOUR_OF_DAY)
                    .optionalEnd()
                    .optionalStart()
                        .appendLiteral(':')
                        .appendValue(ChronoField.MINUTE_OF_HOUR)
                    .optionalEnd()
                    .optionalStart()
                        .appendLiteral(':')
                        .appendValue(ChronoField.SECOND_OF_MINUTE)
                        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .optionalEnd()
                    .parseLenient()
                    .optionalStart()
                        .appendOffsetId()
                    .optionalEnd()
                    .parseStrict()
                    .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
                    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                    .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                    .toFormatter()
                    .withZone(ZoneId.systemDefault());
            // @formatter:on

    public static Date parseDate(String dateString) {
        return Date.from(defaultDateFormatter.parse(dateString, Instant::from));
    }

    public static Calendar parseCalendar(String dateString, ZoneId zoneId) {
        return GregorianCalendar.from(defaultDateFormatter.withZone(zoneId).parse(dateString, ZonedDateTime::from));
    }
}
