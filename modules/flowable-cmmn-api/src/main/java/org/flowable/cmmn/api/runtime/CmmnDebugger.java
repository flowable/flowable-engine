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

/**
 * @author martin.grofcik
 */
public interface CmmnDebugger {
    /**
     * Indicates that {@link PlanItemInstance} is in the breakpoint state
     *
     * @param entryCriterionId execution to evaluate
     * @param planItemInstance executed plan item instance
     *
     * @return true in the case when breakpoint was reached, false in the case when not
     * @throws RuntimeException in the case when it was not possible to decide
     */
    boolean isBreakPoint(String entryCriterionId, PlanItemInstance planItemInstance);
}
