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

import org.flowable.bpm.model.bpmn.impl.instance.Source;
import org.flowable.bpm.model.bpmn.impl.instance.Target;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class LinkEventDefinitionTest
        extends AbstractEventDefinitionTest {

    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return Arrays.asList(
                new ChildElementAssumption(Source.class),
                new ChildElementAssumption(Target.class, 0, 1));
    }

    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Collections.singletonList(
                new AttributeAssumption("name", false, true));
    }

    @Test
    public void getEventDefinition() {
        LinkEventDefinition eventDefinition = eventDefinitionQuery.filterByType(LinkEventDefinition.class).singleResult();
        assertThat(eventDefinition).isNotNull();
        assertThat(eventDefinition.getName()).isEqualTo("link");
        assertThat(eventDefinition.getSources().iterator().next().getName()).isEqualTo("link");
        assertThat(eventDefinition.getTarget().getName()).isEqualTo("link");
    }

}
