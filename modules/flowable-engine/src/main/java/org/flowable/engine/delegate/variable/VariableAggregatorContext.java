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
package org.flowable.engine.delegate.variable;

import org.flowable.bpmn.model.VariableAggregationDefinition;

/**
 * @author Filip Hrisafov
 */
public interface VariableAggregatorContext {

    /**
     * State when the execution is completed and the aggregation needs to be prepared.
     * e.g. This can be done for different executions:
     * <ul>
     *     <li>After the child execution of a multi instance execution completes</li>
     *     <li>After the multi instance execution completes</li>
     *     <li>After any execution completes</li>
     * </ul>
     */
    String COMPLETE = "complete";
    /**
     * State when the execution is not yet completed and we need to see an overview state.
     * e.g. This can be done for different executions:
     * <ul>
     *     <li>For an active child execution of a multi instance execution</li>
     *     <li>For an active multi instance execution</li>
     *     <li>For any execution</li>
     * </ul>
     */
    String OVERVIEW = "overview";

    VariableAggregationDefinition getDefinition();

    String getState();
}
