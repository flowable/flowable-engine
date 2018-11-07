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
package org.flowable.engine.test.api.event;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.variable.api.event.FlowableVariableEvent;

public class TestVariableEventListener extends AbstractFlowableEngineEventListener {

    private List<FlowableEvent> eventsReceived;

    public TestVariableEventListener() {
        eventsReceived = new ArrayList<>();
    }

    public List<FlowableEvent> getEventsReceived() {
        return eventsReceived;
    }

    public void clearEventsReceived() {
        eventsReceived.clear();
    }

    @Override
    protected void variableCreated(FlowableVariableEvent event) {
        eventsReceived.add(event);
    }

    @Override
    protected void variableUpdatedEvent(FlowableVariableEvent event) {
        eventsReceived.add(event);
    }

    @Override
    protected void variableDeletedEvent(FlowableVariableEvent event) {
        eventsReceived.add(event);
    }

    @Override
    public boolean isFailOnException() {
        return true;
    }

}
