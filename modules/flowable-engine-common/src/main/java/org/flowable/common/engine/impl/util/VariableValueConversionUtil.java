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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.flowable.common.engine.api.FlowableException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Utility class for converting variable values between types.
 * Used by IO parameter processing in both the BPMN and CMMN engines.
 *
 * @author Tijs Rademakers
 */
public class VariableValueConversionUtil {

    /**
     * Convert a value to the specified type.
     *
     * @param value the value to convert (must not be null)
     * @param type the target type name (e.g. "string", "integer", "date", "json")
     * @param objectMapper the ObjectMapper to use for JSON/array conversions (may be null if not converting to json/array)
     * @return the converted value
     */
    public static Object convertValue(Object value, String type, ObjectMapper objectMapper) {
        return switch (type.toLowerCase()) {
            case "string" -> convertToString(value);
            case "integer", "int" -> convertToInteger(value);
            case "long" -> convertToLong(value);
            case "double" -> convertToDouble(value);
            case "boolean" -> convertToBoolean(value);
            case "date" -> convertToDate(value);
            case "localdate" -> convertToLocalDate(value);
            case "json" -> convertToJson(value, objectMapper);
            case "array" -> convertToArray(value, objectMapper);
            default -> throw new FlowableException("Unsupported IO parameter type '" + type + "'");
        };
    }

    public static String convertToString(Object value) {
        if (value instanceof String stringValue) {
            return stringValue;
        }
        if (value instanceof JsonNode jsonNode) {
            if (jsonNode.isString()) {
                return jsonNode.asString();
            }
            return jsonNode.toString();
        }
        return value.toString();
    }

    public static Integer convertToInteger(Object value) {
        if (value instanceof Integer intValue) {
            return intValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue) {
            return Integer.valueOf(stringValue.trim());
        }
        throw new FlowableException("Cannot convert value of type " + value.getClass().getName() + " to Integer");
    }

    public static Long convertToLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue) {
            return Long.valueOf(stringValue.trim());
        }
        throw new FlowableException("Cannot convert value of type " + value.getClass().getName() + " to Long");
    }

    public static Double convertToDouble(Object value) {
        if (value instanceof Double doubleValue) {
            return doubleValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String stringValue) {
            return Double.valueOf(stringValue.trim());
        }
        throw new FlowableException("Cannot convert value of type " + value.getClass().getName() + " to Double");
    }

    public static Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.valueOf(stringValue.trim());
        }
        throw new FlowableException("Cannot convert value of type " + value.getClass().getName() + " to Boolean");
    }

    public static Date convertToDate(Object value) {
        if (value instanceof Date dateValue) {
            return dateValue;
        }
        if (value instanceof Instant instant) {
            return Date.from(instant);
        }
        if (value instanceof LocalDate localDate) {
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof LocalDateTime localDateTime) {
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof ZonedDateTime zonedDateTime) {
            return Date.from(zonedDateTime.toInstant());
        }
        if (value instanceof String stringValue) {
            String trimmed = stringValue.trim();
            if (trimmed.startsWith("P")) {
                return Date.from(addDurationToNow(trimmed).toInstant());
            }
            return DateUtil.parseDate(trimmed);
        }
        throw new FlowableException("Cannot convert value of type " + value.getClass().getName() + " to Date");
    }

    public static LocalDate convertToLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (value instanceof Instant instant) {
            return instant.atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.toLocalDate();
        }
        if (value instanceof String stringValue) {
            String trimmed = stringValue.trim();
            if (trimmed.startsWith("P")) {
                return addDurationToNow(trimmed).toLocalDate();
            }
            return DateUtil.parseDate(trimmed).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        throw new FlowableException("Cannot convert value of type " + value.getClass().getName() + " to LocalDate");
    }

    public static ObjectNode convertToJson(Object value, ObjectMapper objectMapper) {
        if (value instanceof ObjectNode objectNode) {
            return objectNode;
        }
        if (value instanceof String stringValue) {
            JsonNode jsonNode = objectMapper.readTree(stringValue);
            if (jsonNode instanceof ObjectNode objectNode) {
                return objectNode;
            }
            throw new FlowableException("JSON string does not represent an object: " + stringValue);
        }
        throw new FlowableException("Cannot convert value of type " + value.getClass().getName() + " to JSON object");
    }

    /**
     * Parses an ISO 8601 duration/period string (e.g. "P10D", "PT10H", "P1Y2M3DT4H") and adds it to the current time.
     * Uses the same parsing logic as {@link org.flowable.common.engine.impl.calendar.DueDateBusinessCalendar}.
     */
    protected static ZonedDateTime addDurationToNow(String durationString) {
        ZonedDateTime now = ZonedDateTime.now();
        Period period;
        Duration duration;
        if (durationString.startsWith("PT")) {
            period = Period.ZERO;
            duration = Duration.parse(durationString);
        } else {
            int timeIndex = durationString.indexOf('T');
            if (timeIndex > 0) {
                period = Period.parse(durationString.substring(0, timeIndex));
                duration = Duration.parse("P" + durationString.substring(timeIndex));
            } else {
                period = Period.parse(durationString);
                duration = Duration.ZERO;
            }
        }
        return now.plus(period).plus(duration);
    }

    public static ArrayNode convertToArray(Object value, ObjectMapper objectMapper) {
        if (value instanceof ArrayNode arrayNode) {
            return arrayNode;
        }
        if (value instanceof String stringValue) {
            JsonNode jsonNode = objectMapper.readTree(stringValue);
            if (jsonNode instanceof ArrayNode arrayNode) {
                return arrayNode;
            }
            throw new FlowableException("JSON string does not represent an array: " + stringValue);
        }
        throw new FlowableException("Cannot convert value of type " + value.getClass().getName() + " to JSON array");
    }

}
