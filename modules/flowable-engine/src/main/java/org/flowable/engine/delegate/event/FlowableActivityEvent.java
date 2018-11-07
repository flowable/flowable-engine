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

/**
 * An {@link FlowableEvent} related to an activity within an execution.
 * 
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public interface FlowableActivityEvent extends FlowableEngineEvent {

    /**
     * @return the id of the activity this event is related to. This corresponds to an id defined in the process definition.
     */
    String getActivityId();

    /**
     * @return the name of the activity this event is related to.
     */
    String getActivityName();

    /**
     * @return the type of the activity (if set during parsing).
     */
    String getActivityType();

    /**
     * @return the behaviourclass of the activity (if it could be determined)
     */
    String getBehaviorClass();

}
