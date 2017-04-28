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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;


public class BaseElementTest
        extends BpmnModelElementInstanceTest {

    @Override
    public TypeAssumption getTypeAssumption() {
        return new TypeAssumption(true);
    }

    @Override
    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return Arrays.asList(
                new ChildElementAssumption(Documentation.class),
                new ChildElementAssumption(ExtensionElements.class, 0, 1));
    }

    @Override
    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Collections.singletonList(
                new AttributeAssumption("id", true));
    }

    @Test
    public void id() {
        Task task = modelInstance.newInstance(Task.class);
        assertThat(task.getId()).isNotNull().startsWith("task");
        task.setId("test");
        assertThat(task.getId()).isEqualTo("test");
        StartEvent startEvent = modelInstance.newInstance(StartEvent.class);
        assertThat(startEvent.getId()).isNotNull().startsWith("startEvent");
        startEvent.setId("test");
        assertThat(startEvent.getId()).isEqualTo("test");
    }

}
