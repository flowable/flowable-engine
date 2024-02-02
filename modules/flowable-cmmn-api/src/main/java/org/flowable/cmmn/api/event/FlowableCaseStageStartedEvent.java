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
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;

/**
 * An event representing a CMMN case stage being started.
 *
 * @author Micha Kiener
 */
public interface FlowableCaseStageStartedEvent extends FlowableEngineEntityEvent {

    /**
     * Returns the case instance the stage belongs to.
     * @return the case instance
     */
    CaseInstance getCaseInstance();

    /**
     * Overwritten in order to return the stage plan item instance.
     * @return the stage plan item instance
     */
    @Override
    PlanItemInstance getEntity();
}
