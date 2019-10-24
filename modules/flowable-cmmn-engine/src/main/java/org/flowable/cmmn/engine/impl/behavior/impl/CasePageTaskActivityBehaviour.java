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
package org.flowable.cmmn.engine.impl.behavior.impl;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.model.CasePageTask;
import org.flowable.common.engine.impl.interceptor.CommandContext;

public class CasePageTaskActivityBehaviour extends TaskActivityBehavior implements PlanItemActivityBehavior {

    protected CasePageTask casePageTask;

    public CasePageTaskActivityBehaviour(CasePageTask casePageTask) {
        super(true, null);
        this.casePageTask = casePageTask;
    }

    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {
        
    }

}
