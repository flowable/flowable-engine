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

import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.DelegateExecution;

/**
 * Event interface for {@link FlowableEvent} implementations related to the process engine,
 * exposing process engine specific functions.
 * 
 * @author Joram Barrez
 */
public interface FlowableProcessEngineEvent extends FlowableEngineEvent {
    
    /**
     * Return the execution this event is associated with. Returns null, if the event was not related to an active execution.
     * 
     * Note that this will only retun a {@link DelegateExecution} instance when a command context is active.
     */
    DelegateExecution getExecution();

}
