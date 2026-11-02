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

package org.flowable.common.rest.api;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.time.FastDateFormat;

/**
 * @author Tijs Rademakers
 */
public class RequestUtil {

    private static final FastDateFormat shortDateFormat = FastDateFormat.getInstance("yyyy-MM-dd");
    private static final FastDateFormat longDateFormatOutputFormater = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ssz");

    private static final DateTimeFormatter longDateFormat =
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd")
                    .optionalStart()
                    .appendLiteral('T')
                    .appendPattern("HH:mm")
                    .optionalStart()
                    .appendPattern(":ss")
                    .optionalStart()
                    .appendPattern(".SSS")
                    .optionalEnd()
                    .optionalEnd()
                    .optionalStart()
                    .appendOffset("+HH:mm", "Z")
                    .optionalEnd()
                    .optionalEnd()
                    .parseDefaulting(ChronoField.HOUR_OF_DAY,     0)
                    .parseDefaulting(ChronoField.MINUTE_OF_HOUR,   0)
                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                    .parseDefaulting(ChronoField.OFFSET_SECONDS,   0)
                    .toFormatter();

    public static boolean getBoolean(Map<String, String> requestParams, String name, boolean defaultValue) {
        boolean value = defaultValue;
        if (requestParams.get(name) != null) {
            value = Boolean.valueOf(requestParams.get(name));
        }
        return value;
    }

    public static int getInteger(Map<String, String> requestParams, String name, int defaultValue) {
        int value = defaultValue;
        if (requestParams.get(name) != null) {
            value = Integer.valueOf(requestParams.get(name));
        }
        return value;
    }

    public static Date getDate(Map<String, String> requestParams, String name) {
        if (requestParams != null && name != null) {
            return parseLongDate(requestParams.get(name));
        }
        return null;
    }

    public static Date parseLongDate(String aDate) {
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(aDate, longDateFormat);
        return Date.from(offsetDateTime.toInstant());
    }

    public static String dateToString(Date date) {
        String dateString = null;
        if (date != null) {
            dateString = longDateFormatOutputFormater.format(date);
        }
        return dateString;
    }

    public static Integer parseToInteger(String integer) {
        Integer parsedInteger = null;
        try {
            parsedInteger = Integer.parseInt(integer);
        } catch (Exception e) {
        }
        return parsedInteger;
    }

    public static Date parseToDate(String date) {
        Date parsedDate = null;
        try {
            parsedDate = shortDateFormat.parse(date);
        } catch (Exception e) {
        }
        return parsedDate;
    }

    public static List<String> parseToList(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String[] valueParts = value.split(",");
        List<String> values = new ArrayList<>(valueParts.length);
        Collections.addAll(values, valueParts);
        return values;
    }

    public static Set<String> parseToSet(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String[] valueParts = value.split(",");
        Set<String> values = new HashSet<>(valueParts.length);
        Collections.addAll(values, valueParts);
        return values;
    }
}
