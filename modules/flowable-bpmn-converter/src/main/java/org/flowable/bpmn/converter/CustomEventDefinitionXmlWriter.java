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
package org.flowable.bpmn.converter;

import javax.xml.stream.XMLStreamWriter;

import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;

/**
 * Hook for serializing a {@link org.flowable.bpmn.model.CustomBpmnEventDefinition} back to XML — the
 * write-side counterpart to {@link org.flowable.bpmn.converter.child.BaseChildElementParser} on the read
 * side. A custom {@link EventDefinition} requires one parser (read) and one writer (write) to round-trip
 * through {@code BpmnXMLConverter}. The writer is invoked inside an already-opened
 * {@code <extensionElements>} wrapper.
 */
@FunctionalInterface
public interface CustomEventDefinitionXmlWriter {

    void write(Event parentEvent, EventDefinition eventDefinition, XMLStreamWriter xtw) throws Exception;
}
