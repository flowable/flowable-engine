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
package org.flowable.bpmn.converter.child;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventRegistryEventDefinition;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.StartEvent;

/**
 * Reads {@code <flowable:eventType>X</flowable:eventType>} as a child of an event host (start /
 * intermediate-catch / boundary) and produces a typed {@link EventRegistryEventDefinition} on the host's
 * {@code eventDefinitions} list. The {@link EventRegistryEventDefinition} is the single source of truth in
 * the model — the legacy XML form is re-emitted on serialization by the host's XML writer, no parallel
 * extension-element representation is kept.
 * <p>
 * On non-event hosts (e.g. {@code receiveTask}, {@code sendEventServiceTask}, {@code process}) this parser
 * declines via {@link #accepts(BaseElement)} so {@code BpmnXMLUtil.parseChildElements} falls through to the
 * generic extension-element path and existing behavior is preserved.
 */
public class EventRegistryEventTypeParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return ELEMENT_EVENT_TYPE;
    }

    @Override
    public boolean accepts(BaseElement element) {
        return element instanceof IntermediateCatchEvent
                || element instanceof BoundaryEvent
                || element instanceof StartEvent;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
        if (!(parentElement instanceof Event event)) {
            return;
        }
        // An empty <flowable:eventType> on an event host is almost always an authoring mistake — warn so it
        // surfaces at parse time rather than failing silently at runtime.
        String eventTypeValue = xtr.getElementText();
        if (StringUtils.isEmpty(eventTypeValue)) {
            LOGGER.warn("Empty <flowable:eventType> extension element on event '{}'; ignoring (no event-registry subscription will be created)",
                    event.getId());
            return;
        }
        if (event.getEventDefinitions().stream().anyMatch(EventRegistryEventDefinition.class::isInstance)) {
            LOGGER.warn("Multiple <flowable:eventType> extension elements on event '{}'; only the first is used and the duplicate value '{}' is ignored",
                    event.getId(), eventTypeValue);
            return;
        }
        event.addEventDefinition(new EventRegistryEventDefinition(eventTypeValue));
    }
}
