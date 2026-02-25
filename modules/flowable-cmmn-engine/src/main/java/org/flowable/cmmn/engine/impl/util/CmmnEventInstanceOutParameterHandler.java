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
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.variable.api.delegate.VariableScope;

public class CmmnEventInstanceOutParameterHandler {

    /**
     * Processes the 'out parameters' of an {@link EventInstance} and stores the corresponding variables on the {@link VariableScope}.
     * Typically used when mapping incoming event payload into a runtime instance.
     */
    public void handleOutParameters(VariableScope variableScope, BaseElement baseElement, EventInstance eventInstance) {
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
                    boolean isTransient = Boolean.parseBoolean(outParameter.getAttributeValue(null, "transient"));
                    Object value = payloadInstance != null ? payloadInstance.getValue() : null;
                    if (isTransient) {
                        variableScope.setTransientVariable(variableName, value);
                    } else {
                        variableScope.setVariable(variableName, value);
                    }
                }
            }
        }
    }
}
