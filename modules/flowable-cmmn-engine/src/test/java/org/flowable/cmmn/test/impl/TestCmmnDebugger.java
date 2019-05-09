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
package org.flowable.cmmn.test.impl;

import java.util.function.BiFunction;

import org.flowable.cmmn.api.runtime.CmmnDebugger;
import org.flowable.cmmn.api.runtime.PlanItemInstance;

/**
 * @author martin.grofcik
 */
public class TestCmmnDebugger implements CmmnDebugger {

    protected static final BiFunction<String, PlanItemInstance, Boolean> FALSE = (entryCriterionId, planItemInstance) -> false;

    protected BiFunction<String, PlanItemInstance, Boolean> isBreakPointFunction = FALSE;

    @Override
    public boolean isBreakPoint(String entryCriterionId, PlanItemInstance planItemInstance) {
        return isBreakPointFunction.apply(entryCriterionId, planItemInstance);
    }

    void setBreakPointPredicate(BiFunction<String, PlanItemInstance, Boolean> isBreakPointPredicate) {
        this.isBreakPointFunction = isBreakPointPredicate;
    }

    void reset() {
        this.isBreakPointFunction = FALSE;
    }
}
