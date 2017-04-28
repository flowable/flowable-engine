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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;


public class UserTaskTest
        extends BpmnModelElementInstanceTest {

    public TypeAssumption getTypeAssumption() {
        return new TypeAssumption(Task.class, false);
    }

    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return Collections.singletonList(
                new ChildElementAssumption(Rendering.class));
    }

    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Arrays.asList(
                new AttributeAssumption("implementation", false, false, "##unspecified"),
                /* Flowable extensions */
                new AttributeAssumption(FLOWABLE_NS, "assignee"),
                new AttributeAssumption(FLOWABLE_NS, "candidateGroups"),
                new AttributeAssumption(FLOWABLE_NS, "candidateUsers"),
                new AttributeAssumption(FLOWABLE_NS, "dueDate"),
                new AttributeAssumption(FLOWABLE_NS, "formHandlerClass"),
                new AttributeAssumption(FLOWABLE_NS, "formKey"),
                new AttributeAssumption(FLOWABLE_NS, "priority"));
    }
}
