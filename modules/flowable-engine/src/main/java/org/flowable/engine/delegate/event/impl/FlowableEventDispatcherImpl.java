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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventDispatcher;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.common.api.delegate.event.FlowableEventType;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.delegate.event.ActivitiEngineEvent;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;

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

    if (event.getType() == FlowableEngineEventType.ENTITY_DELETED && event instanceof FlowableEntityEvent) {
      FlowableEntityEvent entityEvent = (FlowableEntityEvent) event;
      if (entityEvent.getEntity() instanceof ProcessDefinition) {
        // process definition deleted event doesn't need to be dispatched to event listeners
        return;
      }
    }
    
    // Try getting hold of the Process definition, based on the process definition key, if a context is active
    CommandContext commandContext = Context.getCommandContext();
    if (commandContext != null) {
      BpmnModel bpmnModel = extractBpmnModelFromEvent(event);
      if (bpmnModel != null) {
        ((FlowableEventSupport) bpmnModel.getEventSupport()).dispatchEvent(event);
      }
    }
    
  }

  /**
   * In case no process-context is active, this method attempts to extract a process-definition based on the event. In case it's an event related to an entity, this can be deducted by inspecting the
   * entity, without additional queries to the database.
   * 
   * If not an entity-related event, the process-definition will be retrieved based on the processDefinitionId (if filled in). This requires an additional query to the database in case not already
   * cached. However, queries will only occur when the definition is not yet in the cache, which is very unlikely to happen, unless evicted.
   * 
   * @param event
   * @return
   */
  protected BpmnModel extractBpmnModelFromEvent(FlowableEvent event) {
    BpmnModel result = null;
    
    if (result == null && event instanceof ActivitiEngineEvent && ((ActivitiEngineEvent) event).getProcessDefinitionId() != null) {
      ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(
          ((ActivitiEngineEvent) event).getProcessDefinitionId(), true);
      
      if (processDefinition != null) {
        result = Context.getProcessEngineConfiguration().getDeploymentManager().resolveProcessDefinition(processDefinition).getBpmnModel();
      }
    }
    
    return result;
  }

}
