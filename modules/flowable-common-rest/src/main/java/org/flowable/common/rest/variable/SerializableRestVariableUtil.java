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

import java.util.concurrent.atomic.AtomicBoolean;

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
}
