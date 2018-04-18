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
package org.flowable.common.engine.impl.event;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;

/**
 * Base class for all {@link FlowableEvent} implementations, related to entities.
 * 
 * @author Frederik Heremans
 */
public class FlowableEntityEventImpl extends FlowableEventImpl implements FlowableEngineEntityEvent {

    protected Object entity;

    public FlowableEntityEventImpl(Object entity, FlowableEngineEventType type) {
        super(type);
        if (entity == null) {
            throw new FlowableIllegalArgumentException("Entity cannot be null.");
        }
        this.entity = entity;
    }

    @Override
    public Object getEntity() {
        return entity;
    }
}
