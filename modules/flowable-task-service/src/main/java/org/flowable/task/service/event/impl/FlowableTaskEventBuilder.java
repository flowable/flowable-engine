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
package org.flowable.task.service.event.impl;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.event.FlowableEngineEventImpl;
import org.flowable.common.engine.impl.event.FlowableEntityEventImpl;
import org.flowable.task.api.Task;

/**
 * Builder class used to create {@link FlowableEvent} implementations.
 *
 * @author Frederik Heremans
 */
public class FlowableTaskEventBuilder {

    /**
     * @param type
     *            type of event
     * @param entity
     *            the entity this event targets
     * @return an {@link FlowableEntityEvent}.
     */
    public static FlowableEntityEvent createEntityEvent(FlowableEngineEventType type, Object entity) {
        FlowableEntityEventImpl newEvent = new FlowableEntityEventImpl(entity, type);
        populateEventWithCurrentContext(newEvent);
        return newEvent;
    }
    
    protected static void populateEventWithCurrentContext(FlowableEngineEventImpl event) {
        if (event instanceof FlowableEntityEvent) {
            Object persistedObject = ((FlowableEntityEvent) event).getEntity();
            if (persistedObject instanceof Task) {
                Task taskObject = (Task) persistedObject;
                if (taskObject.getScopeType() == null) {
                    event.setProcessInstanceId(taskObject.getProcessInstanceId());
                    event.setExecutionId(taskObject.getExecutionId());
                    event.setProcessDefinitionId(taskObject.getProcessDefinitionId());
                } else {
                    event.setScopeType(taskObject.getScopeType());
                    event.setScopeId(taskObject.getScopeId());
                    event.setSubScopeId(taskObject.getSubScopeId());
                    event.setScopeDefinitionId(taskObject.getScopeDefinitionId());
                }
            }
        }
    }
}
