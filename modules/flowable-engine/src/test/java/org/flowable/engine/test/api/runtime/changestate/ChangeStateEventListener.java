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
package org.flowable.engine.test.api.runtime.changestate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.FlowableActivityCancelledEvent;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.delegate.event.FlowableMessageEvent;
import org.flowable.engine.delegate.event.FlowableSignalEvent;
import org.flowable.variable.api.event.FlowableVariableEvent;

public class ChangeStateEventListener extends AbstractFlowableEngineEventListener {

    private List<FlowableEvent> events = new ArrayList<>();

    public ChangeStateEventListener() {

    }

    @Override
    protected void activityStarted(FlowableActivityEvent event) {
        List<String> types = Arrays.asList("userTask", "subProcess", "callActivity");

        if (types.contains(event.getActivityType())) {
            events.add(event);
        }
    }

    @Override
    protected void activityCancelled(FlowableActivityCancelledEvent event) {
        List<String> types = Arrays.asList("userTask", "subProcess", "callActivity");

        if (types.contains(event.getActivityType())) {
            events.add(event);
        }
    }

    @Override
    protected void timerScheduled(FlowableEngineEntityEvent event) {
        events.add(event);
    }

    @Override
    protected void processCreated(FlowableEngineEntityEvent event) {
        events.add(event);
    }

    @Override
    protected void jobCancelled(FlowableEngineEntityEvent event) {
        events.add(event);
    }

    @Override
    protected void processCancelled(FlowableCancelledEvent event) {
        events.add(event);
    }

    @Override
    protected void variableUpdatedEvent(FlowableVariableEvent event) {
        events.add(event);
    }

    @Override
    protected void variableCreated(FlowableVariableEvent event) {
        events.add(event);
    }

    @Override
    protected void activitySignalWaiting(FlowableSignalEvent event) {
        events.add(event);
    }

    @Override
    protected void activitySignaled(FlowableSignalEvent event) {
        events.add(event);
    }

    @Override
    protected void activityMessageWaiting(FlowableMessageEvent event) {
        events.add(event);
    }

    @Override
    protected void activityMessageReceived(FlowableMessageEvent event) {
        events.add(event);
    }

    @Override
    protected void activityMessageCancelled(FlowableMessageEvent event) {
        events.add(event);
    }

    public void clear() {
        events.clear();
    }

    public Iterator<FlowableEvent> iterator() {
        return events.iterator();
    }

    public List<FlowableEvent> getEvents() {
        return events;
    }

    public boolean hasEvents() {
        return !events.isEmpty();
    }

    public int eventCount() {
        return events.size();
    }
}

