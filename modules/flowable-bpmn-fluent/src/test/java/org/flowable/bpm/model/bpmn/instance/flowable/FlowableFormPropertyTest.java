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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import org.flowable.bpm.model.bpmn.instance.BpmnModelElementInstanceTest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class FlowableFormPropertyTest
        extends BpmnModelElementInstanceTest {

    public TypeAssumption getTypeAssumption() {
        return new TypeAssumption(FLOWABLE_NS, false);
    }

    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return Collections.singletonList(
                new ChildElementAssumption(FLOWABLE_NS, FlowableValue.class));
    }

    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Arrays.asList(
                new AttributeAssumption(FLOWABLE_NS, "id"),
                new AttributeAssumption(FLOWABLE_NS, "name"),
                new AttributeAssumption(FLOWABLE_NS, "type"),
                new AttributeAssumption(FLOWABLE_NS, "required", false, false, false),
                new AttributeAssumption(FLOWABLE_NS, "readable", false, false, true),
                new AttributeAssumption(FLOWABLE_NS, "writeable", false, false, true),
                new AttributeAssumption(FLOWABLE_NS, "variable"),
                new AttributeAssumption(FLOWABLE_NS, "expression"));
    }
}
