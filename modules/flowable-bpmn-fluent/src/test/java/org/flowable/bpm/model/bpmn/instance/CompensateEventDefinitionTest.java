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

public class CompensateEventDefinitionTest
        extends AbstractEventDefinitionTest {

    @Override
    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Arrays.asList(
                new AttributeAssumption("waitForCompletion"),
                new AttributeAssumption("activityRef"));
    }

    @Test
    public void getEventDefinition() {
        CompensateEventDefinition eventDefinition = eventDefinitionQuery.filterByType(CompensateEventDefinition.class).singleResult();
        assertThat(eventDefinition).isNotNull();
        assertThat(eventDefinition.isWaitForCompletion()).isTrue();
        assertThat(eventDefinition.getActivity().getId()).isEqualTo("task");
    }

}
