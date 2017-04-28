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
package org.flowable.bpm.model.bpmn.instance.dc;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DC_NS;

import org.flowable.bpm.model.bpmn.instance.BpmnModelElementInstanceTest;

import java.util.Arrays;
import java.util.Collection;


public class BoundsTest
        extends BpmnModelElementInstanceTest {

    public TypeAssumption getTypeAssumption() {
        return new TypeAssumption(DC_NS, false);
    }

    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return null;
    }

    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Arrays.asList(
                new AttributeAssumption("x", false, true),
                new AttributeAssumption("y", false, true),
                new AttributeAssumption("width", false, true),
                new AttributeAssumption("height", false, true));
    }
}
