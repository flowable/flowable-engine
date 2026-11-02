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
package org.flowable.engine.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventPayloadInstanceImpl;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;
import org.flowable.variable.api.delegate.VariableScope;

public class EventInstanceBpmnUtil {

    /**
     * Reads the 'in parameters' and converts them to {@link EventPayloadInstance} instances.
     * Typically used when needing to create {@link EventInstance}'s and populate the payload.
     */
    public static Collection<EventPayloadInstance> createEventPayloadInstances(VariableScope variableScope, ExpressionManager expressionManager,
            BaseElement baseElement, EventModel eventDefinition) {

        List<EventPayloadInstance> eventPayloadInstances = new ArrayList<>();
        if (baseElement instanceof SendEventServiceTask eventServiceTask) {
            if (!eventServiceTask.getEventInParameters().isEmpty()) {
                for (IOParameter parameter : eventServiceTask.getEventInParameters()) {
                    String sourceValue = null;
                    if (StringUtils.isNotEmpty(parameter.getSourceExpression())) {
                        sourceValue = parameter.getSourceExpression();
                    } else {
                        sourceValue = parameter.getSource();
                    }
                    addEventPayloadInstance(eventPayloadInstances, sourceValue, parameter.getTarget(), 
                                    variableScope, expressionManager, eventDefinition);
                }
            }
            
        } else {
            List<ExtensionElement> inParameters = baseElement.getExtensionElements()
                .getOrDefault(BpmnXMLConstants.ELEMENT_EVENT_IN_PARAMETER, Collections.emptyList());
    
            if (!inParameters.isEmpty()) {
    
                for (ExtensionElement inParameter : inParameters) {
    
                    String sourceExpression = inParameter.getAttributeValue(null, BpmnXMLConstants.ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION);
                    String source = inParameter.getAttributeValue(null, BpmnXMLConstants.ATTRIBUTE_IOPARAMETER_SOURCE);
                    String target = inParameter.getAttributeValue(null, BpmnXMLConstants.ATTRIBUTE_IOPARAMETER_TARGET);
                    
                    String sourceValue = null;
                    if (StringUtils.isNotEmpty(sourceExpression)) {
                        sourceValue = sourceExpression;
                    } else {
                        sourceValue = source;
                    }
    
                    addEventPayloadInstance(eventPayloadInstances, sourceValue, target, variableScope, expressionManager, eventDefinition);
                }
            }
        }

        return eventPayloadInstances;
    }

    protected static void addEventPayloadInstance(List<EventPayloadInstance> eventPayloadInstances, String source, String target,
            VariableScope variableScope, ExpressionManager expressionManager, EventModel eventDefinition) {

        EventPayload eventPayloadDefinition = eventDefinition.getPayload(target);
        if (eventPayloadDefinition != null) {

            Expression sourceExpression = expressionManager.createExpression(source);
            Object value = sourceExpression.getValue(variableScope);

            eventPayloadInstances.add(new EventPayloadInstanceImpl(eventPayloadDefinition, value));
        }
    }

}
