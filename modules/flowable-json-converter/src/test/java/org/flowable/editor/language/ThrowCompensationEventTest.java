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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ThrowEvent;
import org.junit.Test;

/**
 * Created by alireza on 06/11/2016.
 */
public class ThrowCompensationEventTest extends AbstractConverterTest {

    @Override
    protected String getResource() {
        return "test.throwevent.json";
    }

    @Test
    public void throwCompensateEventInJsonShouldGetConvertedToThrowEventWithProperEventDefinition() throws Exception {
        modelShouldHaveAThrowEventContainingCompensationEventDefinition(readJsonFile());
    }

    @Test
    public void ThrowEventContainingCompensationEventDefinitionShouldGetConvertedToThrowCompensateEventInJson() throws Exception {
        modelShouldHaveAThrowEventContainingCompensationEventDefinition(convertToJsonAndBack(readJsonFile()));
    }

    private void modelShouldHaveAThrowEventContainingCompensationEventDefinition(BpmnModel model){
        FlowElement flowElement = model.getMainProcess().getFlowElement("throwCompensationEvent");
        assertNotNull(flowElement);
        assertThat(flowElement, instanceOf(ThrowEvent.class));
        ThrowEvent throwEvent = (ThrowEvent)flowElement;

        final List<EventDefinition> eventDefinitions = throwEvent.getEventDefinitions();
        assertNotNull(eventDefinitions);
        assertEquals(eventDefinitions.size(), 1);
        assertThat(eventDefinitions.get(0), instanceOf(CompensateEventDefinition.class));
        assertEquals(((CompensateEventDefinition)eventDefinitions.get(0)).getActivityRef(), "activity_ref");
    }
}
