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
package org.flowable.cmmn.api.runtime;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface PlanItemInstanceState {

    /*
     * The case states according to the CMMN spec
     */
    String ACTIVE = "active";
    String AVAILABLE = "available";
    String ENABLED = "enabled";
    String DISABLED = "disabled";
    String COMPLETED = "completed";
    String FAILED = "failed";
    String SUSPENDED = "suspended";
    String CLOSED = "closed";
    String TERMINATED = "terminated";
    
    /*
     * Non-spec state, indicating the plan item instance is waiting to be repeated.
     * The repetition will happen when both the repetition rule is resolving to true and a sentry is satisfied.
     * 
     * The reason a plan item instance is created (according to the spec, an instance should be 
     * created only when the sentry is satisfied) is because the local variables (such as repetitionCounter)
     * need an instance to be persisted.
     */
    String WAITING_FOR_REPETITION = "wait_repetition";
    
    /*
     * Non-spec state, indicating the plan item instance is scheduled to be made ACTIVE asynchronously.
     */
    String ASYNC_ACTIVE = "async-active";

    Set<String> EVALUATE_ENTRY_CRITERIA_STATES = new HashSet<>(Arrays.asList(AVAILABLE, WAITING_FOR_REPETITION));
    
    Set<String> END_STATES = new HashSet<>(Arrays.asList(DISABLED, COMPLETED, TERMINATED, FAILED, WAITING_FOR_REPETITION));
    
}
