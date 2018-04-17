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

import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;

public class TestFlowableEntityEventListener extends AbstractFlowableEngineEventListener {

    private List<FlowableEvent> eventsReceived;
    private Class<?> entityClass;

    public TestFlowableEntityEventListener(Class<?> entityClass) {
        this.entityClass = entityClass;

        eventsReceived = new ArrayList<>();
    }

    public List<FlowableEvent> getEventsReceived() {
        return eventsReceived;
    }

    public void clearEventsReceived() {
        eventsReceived.clear();
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (event instanceof FlowableEntityEvent && entityClass.isAssignableFrom(((FlowableEntityEvent) event).getEntity().getClass())) {
            eventsReceived.add(event);
        }
    }

    @Override
    public boolean isFailOnException() {
        return true;
    }

}
