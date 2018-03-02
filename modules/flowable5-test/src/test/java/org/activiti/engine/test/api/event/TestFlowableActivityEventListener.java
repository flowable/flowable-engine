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
package org.activiti.engine.test.api.event;

import java.util.ArrayList;
import java.util.List;

import org.flowable.engine.common.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.delegate.event.FlowableActivityEvent;

/**
 * Test event listener that only records events related to activities ({@link FlowableActivityEvent}s).
 * 
 * @author Frederik Heremans
 */
public class TestFlowableActivityEventListener implements FlowableEventListener {

    private List<FlowableEvent> eventsReceived;
    private boolean ignoreRawActivityEvents;

    public TestFlowableActivityEventListener(boolean ignoreRawActivityEvents) {
        eventsReceived = new ArrayList<FlowableEvent>();
        this.ignoreRawActivityEvents = ignoreRawActivityEvents;
    }

    public List<FlowableEvent> getEventsReceived() {
        return eventsReceived;
    }

    public void clearEventsReceived() {
        eventsReceived.clear();
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (event instanceof FlowableActivityEvent) {
            if (!ignoreRawActivityEvents || (event.getType() != FlowableEngineEventType.ACTIVITY_STARTED &&
                    event.getType() != FlowableEngineEventType.ACTIVITY_COMPLETED)) {
                eventsReceived.add(event);
            }
        }
    }

    public void setIgnoreRawActivityEvents(boolean ignoreRawActivityEvents) {
        this.ignoreRawActivityEvents = ignoreRawActivityEvents;
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }
    
    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }
}
