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
package org.flowable.cmmn.engine.impl.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Filip Hrisafov
 */
public class CmmnCorrelationUtil {

    public static String getCorrelationKey(String elementName, CommandContext commandContext, BaseElement baseElement) {
        String correlationKey = null;
        List<ExtensionElement> eventCorrelationParamExtensions = baseElement.getExtensionElements()
                .getOrDefault(elementName, Collections.emptyList());
        if (!eventCorrelationParamExtensions.isEmpty()) {

            // Cannot evaluate expressions for start events, hence why values are taken as-is
            Map<String, Object> correlationParameters = new HashMap<>();
            for (ExtensionElement eventCorrelation : eventCorrelationParamExtensions) {
                String name = eventCorrelation.getAttributeValue(null, "name");
                String value = eventCorrelation.getAttributeValue(null, "value");
                correlationParameters.put(name, value);
            }

            correlationKey = CommandContextUtil.getEventRegistry().generateKey(correlationParameters);
        }
        return correlationKey;
    }
}
