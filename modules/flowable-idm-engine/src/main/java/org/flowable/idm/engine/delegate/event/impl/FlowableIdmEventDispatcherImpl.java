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

import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventDispatcher;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.common.api.delegate.event.FlowableEventType;

/**
 * Class capable of dispatching events.
 * 
 * @author Tijs Rademakers
 */
public class FlowableIdmEventDispatcherImpl implements FlowableEventDispatcher {

  protected FlowableIdmEventSupport eventSupport;
  protected boolean enabled = true;

  public FlowableIdmEventDispatcherImpl() {
    eventSupport = new FlowableIdmEventSupport();
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

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
  }

}
