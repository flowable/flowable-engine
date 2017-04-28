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
package org.flowable.bpm.model.bpmn.instance;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMNDI_NS;

import org.flowable.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;

import java.util.Arrays;
import java.util.Collection;


public class DefinitionsTest
        extends BpmnModelElementInstanceTest {

    public TypeAssumption getTypeAssumption() {
        return new TypeAssumption(false);
    }

    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return Arrays.asList(
                new ChildElementAssumption(Import.class),
                new ChildElementAssumption(Extension.class),
                new ChildElementAssumption(RootElement.class),
                new ChildElementAssumption(BPMNDI_NS, BpmnDiagram.class),
                new ChildElementAssumption(Relationship.class));
    }

    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Arrays.asList(
                new AttributeAssumption("id", true),
                new AttributeAssumption("name"),
                new AttributeAssumption("targetNamespace", false, true),
                new AttributeAssumption("expressionLanguage", false, false, "http://www.w3.org/1999/XPath"),
                new AttributeAssumption("typeLanguage", false, false, "http://www.w3.org/2001/XMLSchema"),
                new AttributeAssumption("exporter"),
                new AttributeAssumption("exporterVersion"));
    }
}
