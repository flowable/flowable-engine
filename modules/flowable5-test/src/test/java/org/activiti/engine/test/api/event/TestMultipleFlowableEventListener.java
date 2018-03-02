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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;

public class TestMultipleFlowableEventListener implements FlowableEventListener {

    private List<FlowableEvent> eventsReceived;
    private List<Class<?>> entityClasses;
    private List<Class<?>> eventClasses;

    public TestMultipleFlowableEventListener() {
        eventsReceived = new ArrayList<FlowableEvent>();
    }

    public List<FlowableEvent> getEventsReceived() {
        return eventsReceived;
    }

    public void clearEventsReceived() {
        eventsReceived.clear();
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (isAssignableFrom(eventClasses, event) && isAssignableFrom(entityClasses, ((FlowableEntityEvent) event).getEntity())) {
            eventsReceived.add(event);
        }
    }

    private boolean isAssignableFrom(Collection<Class<?>> classes, Object entity) {
        for (Class<?> itemClass : classes) {
            if (itemClass.isAssignableFrom(entity.getClass()))
                return true;
        }
        return false;
    }

    @Override
    public boolean isFailOnException() {
        return true;
    }

    public void setEntityClasses(Class<?>... entityClasses) {
        this.entityClasses = Arrays.asList(entityClasses);
    }

    public void setEventClasses(Class<?>... eventClasses) {
        this.eventClasses = Arrays.asList(eventClasses);
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
