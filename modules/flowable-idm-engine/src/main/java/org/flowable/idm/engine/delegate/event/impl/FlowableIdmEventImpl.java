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
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.idm.api.event.FlowableIdmEventType;

/**
 * Base class for all {@link FlowableIdmEvent} implementations.
 * 
 * @author Tijs Rademakers
 */
public class FlowableIdmEventImpl implements FlowableEvent {

    protected FlowableIdmEventType type;

    /**
     * Creates a new event implementation, not part of an execution context.
     */
    public FlowableIdmEventImpl(FlowableIdmEventType type) {
        if (type == null) {
            throw new FlowableIllegalArgumentException("type is null");
        }
        this.type = type;
    }

    @Override
    public FlowableIdmEventType getType() {
        return type;
    }

    public void setType(FlowableIdmEventType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return getClass() + " - " + type;
    }

}