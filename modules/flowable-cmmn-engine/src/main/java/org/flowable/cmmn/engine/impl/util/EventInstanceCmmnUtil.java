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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.common.engine.api.eventregistry.runtime.EventInstance;
import org.flowable.common.engine.api.eventregistry.runtime.EventPayloadInstance;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Filip Hrisafov
 */
public class EventInstanceCmmnUtil {

    public static void handleEventInstance(VariableScope variableScope, BaseElement baseElement, EventInstance eventInstance) {
        List<ExtensionElement> outParameters = baseElement.getExtensionElements()
            .getOrDefault(CmmnXmlConstants.ELEMENT_EVENT_OUT_PARAMETER, Collections.emptyList());
        if (!outParameters.isEmpty()) {
            Map<String, EventPayloadInstance> payloadInstances = eventInstance.getPayloadInstances()
                .stream()
                .collect(Collectors.toMap(EventPayloadInstance::getDefinitionName, Function.identity()));

            for (ExtensionElement outParameter : outParameters) {
                String payloadSourceName = outParameter.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_SOURCE);
                EventPayloadInstance payloadInstance = payloadInstances.get(payloadSourceName);
                String variableName = outParameter.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_TARGET);
                if (StringUtils.isNotEmpty(variableName)) {
                    Boolean isTransient = Boolean.valueOf(outParameter.getAttributeValue(null, "transient"));
                    Object value = payloadInstance != null ? payloadInstance.getValue() : null;
                    if (Boolean.TRUE.equals(isTransient)) {
                        variableScope.setTransientVariable(variableName, value);
                    } else {
                        variableScope.setVariable(variableName, value);
                    }
                }
            }
        }
    }

}
