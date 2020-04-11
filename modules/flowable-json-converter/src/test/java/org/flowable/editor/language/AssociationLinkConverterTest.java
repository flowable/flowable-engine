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
package org.flowable.editor.language;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.bpmn.model.Association;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.junit.jupiter.api.Test;

public class AssociationLinkConverterTest extends AbstractConverterTest {

    @Override
    protected String getResource() {
        return "test.association.json";
    }

    @Test
    public void testBoundaryCompensationConverter() throws Exception {
        final BpmnModel bpmnModel = readJsonFile();
        final List<BoundaryEvent> boundaryEvents =
                bpmnModel.getMainProcess().findFlowElementsOfType(BoundaryEvent.class);
        assertThat(boundaryEvents).hasSize(1);
        final List<EventDefinition> eventDefinitions = boundaryEvents.get(0).getEventDefinitions();
        assertThat(eventDefinitions).hasSize(1);
        assertThat(eventDefinitions.get(0)).isInstanceOf(CompensateEventDefinition.class);
        final List<Association> associations =
                bpmnModel.getMainProcess().findAssociationsWithSourceRefRecursive("userTask1");
        assertThat(associations).hasSize(1);
    }
}
