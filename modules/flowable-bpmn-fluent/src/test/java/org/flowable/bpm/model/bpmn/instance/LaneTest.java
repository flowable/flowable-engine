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

import org.flowable.bpm.model.bpmn.impl.instance.ChildLaneSet;
import org.flowable.bpm.model.bpmn.impl.instance.FlowNodeRef;
import org.flowable.bpm.model.bpmn.impl.instance.PartitionElement;

import java.util.Arrays;
import java.util.Collection;


public class LaneTest
        extends BpmnModelElementInstanceTest {

    public TypeAssumption getTypeAssumption() {
        return new TypeAssumption(BaseElement.class, false);
    }

    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return Arrays.asList(
                new ChildElementAssumption(PartitionElement.class, 0, 1),
                new ChildElementAssumption(FlowNodeRef.class),
                new ChildElementAssumption(ChildLaneSet.class, 0, 1));
    }

    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Arrays.asList(
                new AttributeAssumption("name"),
                new AttributeAssumption("partitionElementRef"));
    }
}
