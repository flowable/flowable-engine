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
package org.flowable.common.engine.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.json.FlowableJsonNode;
import org.flowable.common.engine.impl.json.VariableJsonMapper;
import org.flowable.common.engine.impl.util.DateUtil;
import org.flowable.common.engine.impl.util.JsonUtil;

/**
 * Default implementation of {@link VariableValueConversionHandler}.
 *
 * @author Tijs Rademakers
 */
public class DefaultVariableValueConversionHandler implements VariableValueConversionHandler {

    @Override
    public Object convertValue(Object value, String type, VariableJsonMapper variableJsonMapper) {
        return switch (type.toLowerCase()) {
            case "string" -> convertToString(value);
            case "integer", "int" -> convertToInteger(value);
            case "long" -> convertToLong(value);
            case "double" -> convertToDouble(value);
            case "boolean" -> convertToBoolean(value);
            case "date" -> convertToDate(value);
            case "localdate" -> convertToLocalDate(value);
            case "json" -> convertToJson(value, variableJsonMapper);
            case "array" -> convertToArray(value, variableJsonMapper);
            default -> throw new FlowableException("Unsupported IO parameter type '" + type + "'");
        };
    }

    protected String convertToString(Object value) {
        if (value instanceof String stringValue) {
            return stringValue;
        }
        if (value != null && JsonUtil.isJsonNode(value)) {
            FlowableJsonNode jsonNode = JsonUtil.asFlowableJsonNode(value);
            if (jsonNode.isString()) {
                return jsonNode.asString();
            }
            return jsonNode.toString();
        }
        return value.toString();
    }

    protected Integer convertToInteger(Object value) {
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

    protected Long convertToLong(Object value) {
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

    protected Double convertToDouble(Object value) {
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

    protected Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.valueOf(stringValue.trim());
        }
        throw new FlowableException("Cannot convert value of type " + value.getClass().getName() + " to Boolean");
    }

    protected Date convertToDate(Object value) {
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

    protected LocalDate convertToLocalDate(Object value) {
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

    protected Object convertToJson(Object value, VariableJsonMapper variableJsonMapper) {
        if (value != null && JsonUtil.isObjectNode(value)) {
            return value;
        }
        if (value instanceof String stringValue) {
            Object jsonNode = variableJsonMapper.readTree(stringValue);
            if (JsonUtil.isObjectNode(jsonNode)) {
                return jsonNode;
            }
            throw new FlowableException("JSON string does not represent an object: " + stringValue);
        }
        throw new FlowableException("Cannot convert value of type " + value.getClass().getName() + " to JSON object");
    }

    protected Object convertToArray(Object value, VariableJsonMapper variableJsonMapper) {
        if (value != null && JsonUtil.isArrayNode(value)) {
            return value;
        }
        if (value instanceof String stringValue) {
            Object jsonNode = variableJsonMapper.readTree(stringValue);
            if (JsonUtil.isArrayNode(jsonNode)) {
                return jsonNode;
            }
            throw new FlowableException("JSON string does not represent an array: " + stringValue);
        }
        throw new FlowableException("Cannot convert value of type " + value.getClass().getName() + " to JSON array");
    }

    /**
     * Parses an ISO 8601 duration/period string (e.g. "P10D", "PT10H", "P1Y2M3DT4H") and adds it to the current time.
     * Uses the same parsing logic as {@link org.flowable.common.engine.impl.calendar.DueDateBusinessCalendar}.
     */
    protected ZonedDateTime addDurationToNow(String durationString) {
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

}
