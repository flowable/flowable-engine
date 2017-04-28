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
package org.flowable.bpm.model.bpmn.instance.di;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DC_NS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DI_NS;

import org.flowable.bpm.model.bpmn.instance.BpmnModelElementInstanceTest;
import org.flowable.bpm.model.bpmn.instance.dc.Bounds;

import java.util.Collection;
import java.util.Collections;


public class LabelTest
        extends BpmnModelElementInstanceTest {

    public TypeAssumption getTypeAssumption() {
        return new TypeAssumption(DI_NS, Node.class, true);
    }

    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return Collections.singletonList(
                new ChildElementAssumption(DC_NS, Bounds.class, 0, 1));
    }

    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return null;
    }
}
