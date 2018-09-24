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

import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * Class capable of dispatching events.
 * 
 * @author Frederik Heremans
 */
public class FlowableEventDispatcherImpl implements FlowableEventDispatcher {

    protected FlowableEventSupport eventSupport;
    protected boolean enabled = true;

    public FlowableEventDispatcherImpl() {
        eventSupport = new FlowableEventSupport();
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void addEventListener(FlowableEventListener listenerToAdd) {
        eventSupport.addEventListener(listenerToAdd);
    }

    @Override
    public void addEventListener(FlowableEventListener listenerToAdd, FlowableEventType... types) {
        eventSupport.addEventListener(listenerToAdd, types);
    }

    @Override
    public void removeEventListener(FlowableEventListener listenerToRemove) {
        eventSupport.removeEventListener(listenerToRemove);
    }

    @Override
    public void dispatchEvent(FlowableEvent event) {
        if (enabled) {
            eventSupport.dispatchEvent(event);
        }

        CommandContext commandContext = Context.getCommandContext();
        if (commandContext != null) {
            AbstractEngineConfiguration engineConfiguration = commandContext.getCurrentEngineConfiguration();
            if (engineConfiguration != null && engineConfiguration.getAdditionalEventDispatchActions() != null) {
                for (EventDispatchAction eventDispatchAction : engineConfiguration.getAdditionalEventDispatchActions()) {
                    eventDispatchAction.dispatchEvent(commandContext, eventSupport, event);
                }
            }
        }
    }

    public FlowableEventSupport getEventSupport() {
        return eventSupport;
    }

    public void setEventSupport(FlowableEventSupport eventSupport) {
        this.eventSupport = eventSupport;
    }

}
