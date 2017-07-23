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
package org.flowable.engine.delegate.event;

import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventType;
import org.flowable.engine.common.api.delegate.event.TransactionDependentFlowableEventDispatcher;
import org.flowable.engine.common.api.delegate.event.TransactionDependentFlowableEventListener;

/**
 * Class capable of dispatching events.
 *
 * @author Frederik Heremans
 */
public class TransactionDependentFlowableEventDispatcherImpl implements TransactionDependentFlowableEventDispatcher {

    protected TransactionDependentFlowableEventSupport eventSupport;
    protected boolean enabled = true;

    public TransactionDependentFlowableEventDispatcherImpl() {
        eventSupport = new TransactionDependentFlowableEventSupport();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void addEventListener(TransactionDependentFlowableEventListener listenerToAdd) {
        eventSupport.addEventListener(listenerToAdd);
    }

    @Override
    public void addEventListener(TransactionDependentFlowableEventListener listenerToAdd, FlowableEventType... types) {
        eventSupport.addEventListener(listenerToAdd, types);
    }

    @Override
    public void removeEventListener(TransactionDependentFlowableEventListener listenerToRemove) {
        eventSupport.removeEventListener(listenerToRemove);
    }

    @Override
    public void dispatchEvent(FlowableEvent event) {
        if (enabled) {
            eventSupport.dispatchEvent(event);
        }
    }

    public TransactionDependentFlowableEventSupport getEventSupport() {
        return eventSupport;
    }

    public void setEventSupport(TransactionDependentFlowableEventSupport eventSupport) {
        this.eventSupport = eventSupport;
    }

}
