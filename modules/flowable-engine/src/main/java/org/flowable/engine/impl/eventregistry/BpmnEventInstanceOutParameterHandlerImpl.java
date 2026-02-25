package org.flowable.engine.impl.eventregistry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.variable.api.delegate.VariableScope;

public class BpmnEventInstanceOutParameterHandlerImpl implements BpmnEventInstanceOutParameterHandler {

    /**
     * Processes the 'out parameters' of an {@link EventInstance} and stores the corresponding variables on the {@link VariableScope}.
     * Typically used when mapping incoming event payload into a runtime instance (the {@link VariableScope}).
     */
    @Override
    public void handleOutParameters(VariableScope variableScope, BaseElement baseElement, EventInstance eventInstance) {
        Map<String, EventPayloadInstance> payloadInstances = eventInstance.getPayloadInstances()
                .stream()
                .collect(Collectors.toMap(EventPayloadInstance::getDefinitionName, Function.identity()));

        if (baseElement instanceof SendEventServiceTask eventServiceTask) {
            if (!eventServiceTask.getEventOutParameters().isEmpty()) {
                for (IOParameter parameter : eventServiceTask.getEventOutParameters()) {
                    setEventParameterVariable(parameter.getSource(), parameter.getTarget(),
                            parameter.isTransient(), payloadInstances, variableScope);
                }
            }

        } else {
            List<ExtensionElement> outParameters = baseElement.getExtensionElements()
                    .getOrDefault(BpmnXMLConstants.ELEMENT_EVENT_OUT_PARAMETER, Collections.emptyList());
            if (!outParameters.isEmpty()) {
                for (ExtensionElement outParameter : outParameters) {
                    String payloadSourceName = outParameter.getAttributeValue(null, BpmnXMLConstants.ATTRIBUTE_IOPARAMETER_SOURCE);
                    String variableName = outParameter.getAttributeValue(null, BpmnXMLConstants.ATTRIBUTE_IOPARAMETER_TARGET);
                    boolean isTransient = Boolean.parseBoolean(outParameter.getAttributeValue(null, "transient"));
                    setEventParameterVariable(payloadSourceName, variableName, isTransient, payloadInstances, variableScope);
                }
            }
        }
    }

    protected void setEventParameterVariable(String source, String target, boolean isTransient,
            Map<String, EventPayloadInstance> payloadInstances, VariableScope variableScope) {

        EventPayloadInstance payloadInstance = payloadInstances.get(source);
        if (StringUtils.isNotEmpty(target)) {
            Object value = payloadInstance != null ? payloadInstance.getValue() : null;
            if (isTransient) {
                variableScope.setTransientVariable(target, value);
            } else {
                variableScope.setVariable(target, value);
            }
        }
    }
}
