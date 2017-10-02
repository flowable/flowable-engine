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
package org.flowable.engine.common.impl.event;

import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventType;
import org.flowable.engine.common.api.delegate.event.TransactionFlowableEventDispatcher;
import org.flowable.engine.common.api.delegate.event.TransactionFlowableEventListener;

/**
 * Class capable of dispatching events.
 *
 * @author Frederik Heremans
 */
public class TransactionFlowableEventDispatcherImpl implements TransactionFlowableEventDispatcher {

    protected TransactionDependentFlowableEventSupport eventSupport;
    protected boolean enabled = true;

    public TransactionFlowableEventDispatcherImpl() {
        eventSupport = new TransactionDependentFlowableEventSupport();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void addEventListener(TransactionFlowableEventListener listenerToAdd) {
        eventSupport.addEventListener(listenerToAdd);
    }

    @Override
    public void addEventListener(TransactionFlowableEventListener listenerToAdd, FlowableEventType... types) {
        eventSupport.addEventListener(listenerToAdd, types);
    }

    @Override
    public void removeEventListener(TransactionFlowableEventListener listenerToRemove) {
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
