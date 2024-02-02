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
package org.flowable.cmmn.engine.impl.runtime;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.common.engine.impl.callback.CallbackData;

/**
 * @author Joram Barrez
 */
public interface CaseInstanceHelper {
    
    CaseInstanceEntity startCaseInstance(CaseInstanceBuilder caseInstanceBuilder);

    CaseInstanceEntity startCaseInstanceAsync(CaseInstanceBuilder caseInstanceBuilder);

    /**
     * Creates a new case instance within the runtime based on the given historic and ended case instance to be reactivated later on. This method only copies
     * all relevant data like the case instance, its plan items and variables to the runtime, but does not further reactivate plan items or trigger the
     * reactivation listener. But it also sets the state of the runtime case instance to active and keeps the historic one in sync.
     *
     * @param caseInstance the historic case instance to copy back to the runtime
     * @return the copied case instance entity added back to the runtime
     */
    CaseInstanceEntity copyHistoricCaseInstanceToRuntime(HistoricCaseInstance caseInstance);

    void callCaseInstanceStateChangeCallbacks(CallbackData callbackData);
    
}
