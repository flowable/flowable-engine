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
package org.flowable.cmmn.api.delegate;

import org.flowable.cmmn.model.VariableAggregationDefinition;

/**
 * @author Filip Hrisafov
 */
public interface PlanItemVariableAggregatorContext {

    /**
     * State when a plan item instance is completed and the aggregation needs to be prepared.
     * e.g. This can be done for different executions:
     * <ul>
     *     <li>After a repeatable plan item instance completes</li>
     *     <li>After all repeatable plan item instances complete</li>
     *     <li>After any plan item instance completes</li>
     * </ul>
     */
    String COMPLETE = "complete";
    /**
     * State when the plan item instance is not yet completed and we need to see an overview state.
     * e.g. This can be done for different executions:
     * <ul>
     *     <li>For an active plan item instance part of a repeatable set of instances </li>
     *     <li>For any plan item</li>
     * </ul>
     */
    String OVERVIEW = "overview";

    VariableAggregationDefinition getDefinition();

    String getState();
}
