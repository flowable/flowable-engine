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
package org.flowable.bpm.model.bpmn.instance.flowable;

import org.flowable.bpm.model.bpmn.impl.BpmnModelConstants;
import org.flowable.bpm.model.bpmn.instance.BpmnModelElementInstanceTest;
import org.flowable.bpm.model.bpmn.instance.ResourceAssignmentExpression;

import java.util.Collection;
import java.util.Collections;

public class FlowablePotentialStarterTest
        extends BpmnModelElementInstanceTest {
    public TypeAssumption getTypeAssumption() {
        return new TypeAssumption(BpmnModelConstants.FLOWABLE_NS, false);
    }

    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return Collections.singletonList(
                new ChildElementAssumption(ResourceAssignmentExpression.class, 0, 1));
    }

    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return null;
    }
}
