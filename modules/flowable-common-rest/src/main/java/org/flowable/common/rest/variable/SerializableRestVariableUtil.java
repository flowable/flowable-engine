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
package org.flowable.common.rest.variable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.rest.exception.FlowableContentNotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * Utility for determining whether Java serialized objects are allowed as variables in the REST API.
 *
 * @author Filip Hrisafov
 */
public final class SerializableRestVariableUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerializableRestVariableUtil.class);

    public static final String ALLOW_SERIALIZABLE_PROPERTY = "rest.variables.allow.serializable";

    /**
     * The name of the engine variable type that stores values using Java serialization.
     */
    public static final String SERIALIZABLE_VARIABLE_TYPE_NAME = "serializable";

    // The property is read by multiple REST resources, the warning only needs to be logged once
    private static final AtomicBoolean ENABLED_WARNING_LOGGED = new AtomicBoolean();

    private SerializableRestVariableUtil() {
    }

    public static boolean isSerializableVariableAllowed(Environment environment) {
        boolean allowed = environment.getProperty(ALLOW_SERIALIZABLE_PROPERTY, Boolean.class, false);
        if (allowed && ENABLED_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn("Java serialized objects are allowed as variables in the REST API ({}=true). Deserializing Java objects from a request is a security risk,"
                    + " only use this when the REST API is used by fully trusted clients.", ALLOW_SERIALIZABLE_PROPERTY);
        }
        return allowed;
    }

    /**
     * Validates that the data of a variable with the given type can be exposed through the REST API.
     * This is done based on the type of the variable, before its value is resolved,
     * such that no serializable variable is deserialized when serialized objects are not allowed.
     *
     * @throws FlowableContentNotSupportedException when the variable is a serializable variable and serialized objects are not allowed
     */
    public static void validateSerializableVariableAllowed(String variableTypeName, boolean serializableVariableAllowed) {
        if (!serializableVariableAllowed && SERIALIZABLE_VARIABLE_TYPE_NAME.equals(variableTypeName)) {
            throw new FlowableContentNotSupportedException("Serialized objects are not allowed");
        }
    }

    /**
     * Validates the value of a variable for which no type has been provided and that is going to be stored.
     * A JSON object or array without a type is mapped to a {@link Map} or a {@link Collection}, which can only be stored using Java serialization.
     * Such a value is rejected, the json type needs to be used for it instead.
     *
     * @throws FlowableIllegalArgumentException when the value is a JSON object or array
     */
    public static void validateUntypedVariableValue(String variableName, Object value) {
        if (value instanceof Map || value instanceof Collection) {
            throw new FlowableIllegalArgumentException("Variable '" + variableName + "' has a JSON object or array as value, but no type."
                    + " Set the type of the variable to 'json'.");
        }
    }
}
