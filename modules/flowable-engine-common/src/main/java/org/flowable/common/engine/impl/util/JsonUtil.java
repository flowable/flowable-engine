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

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.json.FlowableArrayNode;
import org.flowable.common.engine.impl.json.FlowableJsonNode;
import org.flowable.common.engine.impl.json.jackson2.FlowableJackson2ArrayNode;
import org.flowable.common.engine.impl.json.jackson2.FlowableJackson2JsonNode;
import org.flowable.common.engine.impl.json.jackson3.FlowableJackson3ArrayNode;
import org.flowable.common.engine.impl.json.jackson3.FlowableJackson3JsonNode;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

/**
 * @author Filip Hrisafov
 */
public class JsonUtil {

    private static final boolean JACKSON_2_PRESENT = ReflectUtil.isClassPresent("com.fasterxml.jackson.databind.JsonNode");

    public static FlowableJsonNode asFlowableJsonNode(Object value) {
        if (value == null) {
            return null;
        }
        FlowableJsonNode flowableJsonNode;
        if (value instanceof JsonNode node) {
            flowableJsonNode = FlowableJackson3JsonNode.wrap(node);
        } else if (JACKSON_2_PRESENT) {
            if (value instanceof com.fasterxml.jackson.databind.JsonNode node) {
                flowableJsonNode = FlowableJackson2JsonNode.wrap(node);
            } else {
                flowableJsonNode = null;
            }
        } else {
            flowableJsonNode = null;
        }

        return flowableJsonNode;
    }

    public static FlowableArrayNode asFlowableArrayNode(Object value) {
        if (value == null) {
            return null;
        }

        FlowableArrayNode flowableArrayNode;
        if (value instanceof ArrayNode arrayNode) {
            flowableArrayNode = new FlowableJackson3ArrayNode(arrayNode);
        } else if (JACKSON_2_PRESENT) {
            if (value instanceof com.fasterxml.jackson.databind.node.ArrayNode arrayNode) {
                flowableArrayNode = new FlowableJackson2ArrayNode(arrayNode);
            } else {
                flowableArrayNode = null;
            }
        } else {
            flowableArrayNode = null;
        }

        return flowableArrayNode;
    }

    public static JsonNode asJsonNode(Object value, ObjectMapper objectMapper) {
        return asJsonNode(value, objectMapper, false);
    }

    public static JsonNode asJsonNode(Object value, ObjectMapper objectMapper, boolean allowOtherTypes) {
        if (value instanceof JsonNode jsonNode) {
            return jsonNode;
        }

        if (JACKSON_2_PRESENT) {
            if (value instanceof com.fasterxml.jackson.databind.JsonNode jackson2JsonNode) {
                String jsonString = jackson2JsonNode.toString();
                return objectMapper.readTree(jsonString);
            }
        }
        if (allowOtherTypes) {
            return null;
        }

        throw new FlowableIllegalArgumentException("Cannot convert value of type " + value.getClass() + " to Jackson 3 JsonNode");
    }

    public static Object deepCopyIfJson(Object value) {
        if (value instanceof JsonNode jsonNode) {
            return jsonNode.deepCopy();
        } else if (JACKSON_2_PRESENT) {
            if (value instanceof com.fasterxml.jackson.databind.JsonNode jsonNode) {
                return jsonNode.deepCopy();
            }
        }

        return value;
    }

    public static boolean isArrayNode(Object value) {
        if (value instanceof ArrayNode) {
            return true;
        }
        if (JACKSON_2_PRESENT) {
            return value instanceof com.fasterxml.jackson.databind.node.ArrayNode;
        }

        return false;
    }

    public static boolean isJsonNode(Object value) {
        if (value instanceof JsonNode) {
            return true;
        }

        if (JACKSON_2_PRESENT) {
            return value instanceof com.fasterxml.jackson.databind.JsonNode;
        }

        return false;
    }
}
