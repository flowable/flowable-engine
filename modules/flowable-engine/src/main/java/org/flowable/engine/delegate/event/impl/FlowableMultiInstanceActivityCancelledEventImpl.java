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

package org.flowable.engine.delegate.event.impl;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.FlowableMultiInstanceActivityCancelledEvent;

/**
 * An {@link FlowableMultiInstanceActivityCancelledEvent} implementation.
 *
 * @author Robert Hafner
 */
public class FlowableMultiInstanceActivityCancelledEventImpl extends FlowableMultiInstanceActivityEventImpl implements FlowableMultiInstanceActivityCancelledEvent {
    protected Object cause;

    public FlowableMultiInstanceActivityCancelledEventImpl() {
        super(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_CANCELLED);
    }

    public void setCause(Object cause) {
        this.cause = cause;
    }

    @Override
    public Object getCause() {
        return null;
    }
}
