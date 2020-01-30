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

/**
 * @author Joram Barrez
 */
public interface CaseInstanceState {
    
    /*
     * The case states according to the CMMN spec
     */
    
    String ACTIVE = "active";
    String COMPLETED = "completed";
    String FAILED = "failed";
    String SUSPENDED = "suspended";
    String CLOSED = "closed";
    String TERMINATED = "terminated";
    
    Set<String> END_STATES = new HashSet<>(Arrays.asList(COMPLETED, FAILED, SUSPENDED, CLOSED, TERMINATED));

    static boolean isInTerminalState(CaseInstance caseInstance) {
        return END_STATES.contains(caseInstance.getState());
    }

}
