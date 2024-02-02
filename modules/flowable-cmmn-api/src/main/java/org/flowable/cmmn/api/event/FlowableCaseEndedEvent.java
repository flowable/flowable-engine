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
package org.flowable.cmmn.api.event;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;

/**
 * The interface for a case ended event, which might be a case completed or case terminated event.
 *
 * @author Micha Kiener
 */
public interface FlowableCaseEndedEvent extends FlowableEngineEntityEvent {

    String ENDING_STATE_COMPLETED = CaseInstanceState.COMPLETED;
    String ENDING_STATE_TERMINATED = CaseInstanceState.TERMINATED;

    /**
     * Returns the ending state of the case which can be {@link #ENDING_STATE_COMPLETED} or {@link #ENDING_STATE_TERMINATED}.
     * @return the ending state of the case
     */
    String getEndingState();

    @Override
    CaseInstance getEntity();


}
