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
package org.flowable.idm.engine.delegate.event.impl;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.idm.api.event.FlowableIdmEventType;

/**
 * Base class for all {@link FlowableIdmEntityEvent} implementations, related to entities.
 *
 * @author Tijs Rademakers
 */
public class FlowableIdmEntityEventImpl extends FlowableIdmEventImpl implements FlowableEntityEvent {

    protected Object entity;

    public FlowableIdmEntityEventImpl(Object entity, FlowableIdmEventType type) {
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
