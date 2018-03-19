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
package org.flowable.dmn.engine.impl.el.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Yvo Swillens
 */
public class DMNParseUtil {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DMNParseUtil.class);

    public static void isCollection(Object collection) {
        if (!isJavaCollection(collection) && !isArrayNode(collection)) {
            throw new IllegalArgumentException("collection must be of type java.util.Collection or com.fasterxml.jackson.databind.node.ArrayNode");
        }
    }

    public static boolean isJavaCollection(Object collection) {
        return Collection.class.isAssignableFrom(collection.getClass());
    }

    public static boolean isArrayNode(Object collection) {
        return ArrayNode.class.isAssignableFrom(collection.getClass());
    }

    public static boolean isDMNCollection(Object value) {
        if (value instanceof String == false || value.equals("")) {
            return false;
        }

        String stringValue = String.valueOf(value);
        return stringValue.contains(",");
    }

    public static Collection getCollectionFromDMNCollection(Object value, Collection inputCollection) {
        String stringValue = String.valueOf(value);
        Class<?> collectionType = getCollectionType(inputCollection);
        List<Object> items = split(stringValue, collectionType);
        return items;
    }

    public static Collection getCollectionFromArrayNode(ArrayNode arrayNode) {
        List<Object> values = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            values.add(getJsonValue(node));
        }
        return values;
    }

    protected static Object getJsonValue(JsonNode jsonNode) {
        switch (jsonNode.getNodeType()) {
            case ARRAY:
                LOGGER.warn("Nested ArrayNodes not supported");
            case BINARY:
                LOGGER.warn("Nested BinaryNodes not supported");
            case OBJECT:
                LOGGER.warn("Nested ObjectNodes not supported");
            case POJO:
                LOGGER.warn("Nested PojoNodes not supported");
            case BOOLEAN:
                return jsonNode.booleanValue();
            case NULL:
                return null;
            case NUMBER:
                return getNumberValue(jsonNode.numberValue().toString());
            default:
                return jsonNode.textValue();
        }
    }

    protected static List<Object> split(String str, Class<?> collectionType) {
        return Stream.of(str.split(","))
            .map(elem -> formatElementValue(elem.trim(), collectionType))
            .collect(Collectors.toList());
    }

    protected static Object getFormattedValue(String value, Collection inputCollection) {
        Class<?> collectionType = getCollectionType(inputCollection);
        return formatElementValue(value, collectionType);
    }

    protected static Object formatElementValue(String value, Class<?> collectionType) {
        if (value.isEmpty()) {
            return null;
        }

        value = removedSurroundingQuotes(value);

        // format element based on collection type
        if (Date.class.equals(collectionType)) {
            return DateUtil.toDate(value);
        } else if (LocalDate.class.equals(collectionType)) {
            return new DateTime(DateUtil.toDate(value)).toLocalDate();
        } else if (Integer.class.equals(collectionType) || Long.class.equals(collectionType) || Float.class.equals(collectionType) || Double.class.equals(collectionType)) {
            return getNumberValue(value, collectionType);
        } else if (Boolean.class.equals(collectionType)) {
            return Boolean.valueOf(value);
        } else if (String.class.equals(collectionType)) {
            return value;
        }
        return value;
    }

    protected static String removedSurroundingQuotes(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            String result = value.substring(1, value.length() - 1);
            return result;
        } else {
            return value;
        }
    }

    protected static Object getNumberValue(String value, Class<?> targetType) {
        Object returnValue = null;
        try {
            if (Integer.class.equals(targetType)) {
                returnValue = Integer.valueOf(value);
            } else if (Long.class.equals(targetType)) {
                returnValue = Long.valueOf(value);
            } else if (Float.class.equals(targetType)) {
                returnValue = Float.valueOf(value);
            } else if (Double.class.equals(targetType)) {
                returnValue = Double.valueOf(value);
            }
        } catch (NumberFormatException nfe) {
            LOGGER.warn("Could not parse to Integer, Long, Float or Double from: " + value);
        }

        return returnValue;
    }

    protected static Object getNumberValue(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException nfe1) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException nfe2) {
                LOGGER.warn("Could not parse to Long or Double from: " + value);
                return null;
            }
        }
    }

    protected static Class<?> getCollectionType(Collection collection) {
        if (collection.isEmpty()) {
            return null;
        }
        return collection.iterator().next().getClass();
    }

}
