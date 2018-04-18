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
import org.flowable.engine.delegate.event.FlowableMultiInstanceActivityCompletedEvent;

/**
 * Implementation of an {@link org.flowable.engine.delegate.event.FlowableMultiInstanceActivityCompletedEvent}.
 *
 * @author Robert Hafner
 */
public class FlowableMultiInstanceActivityCompletedEventImpl extends FlowableMultiInstanceActivityEventImpl implements
        FlowableMultiInstanceActivityCompletedEvent {
    protected int numberOfInstances;
    protected int numberOfActiveInstances;
    protected int numberOfCompletedInstances;

    public FlowableMultiInstanceActivityCompletedEventImpl(FlowableEngineEventType type) {
        super(type);
    }

    @Override
    public int getNumberOfInstances() {
        return numberOfInstances;
    }

    public void setNumberOfInstances(int numberOfInstances) {
        this.numberOfInstances = numberOfInstances;
    }

    @Override
    public int getNumberOfActiveInstances() {
        return numberOfActiveInstances;
    }

    public void setNumberOfActiveInstances(int numberOfActiveInstances) {
        this.numberOfActiveInstances = numberOfActiveInstances;
    }

    @Override
    public int getNumberOfCompletedInstances() {
        return numberOfCompletedInstances;
    }

    public void setNumberOfCompletedInstances(int numberOfCompletedInstances) {
        this.numberOfCompletedInstances = numberOfCompletedInstances;
    }
}
