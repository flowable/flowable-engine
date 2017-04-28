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
package org.flowable.bpm.model.bpmn.instance.bpmndi;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMNDI_NS;

import org.flowable.bpm.model.bpmn.instance.BpmnModelElementInstanceTest;
import org.flowable.bpm.model.bpmn.instance.di.LabeledEdge;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;


public class BpmnEdgeTest
        extends BpmnModelElementInstanceTest {

    public TypeAssumption getTypeAssumption() {
        return new TypeAssumption(BPMNDI_NS, LabeledEdge.class, false);
    }

    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return Collections.singletonList(
                new ChildElementAssumption(BPMNDI_NS, BpmnLabel.class, 0, 1));
    }

    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Arrays.asList(
                new AttributeAssumption("bpmnElement"),
                new AttributeAssumption("sourceElement"),
                new AttributeAssumption("targetElement"),
                new AttributeAssumption("messageVisibleKind"));
    }
}
