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
package org.flowable.engine.impl.delegate;

import java.util.List;

import org.flowable.bpmn.model.VariableAggregationDefinition;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.variable.api.persistence.entity.VariableInstance;

/**
 * An interface that can be used to aggregate multiple variables into a single one based on an aggregation definition.
 *
 * @author Filip Hrisafov
 */
public interface VariableAggregator {

    /**
     * Create a single variable value based on the provided aggregation definition.
     *
     * @param execution the delegate execution from where we need to get data from
     * @param context the aggregation context
     * @return the value for the aggregated variable
     */
    Object aggregateSingle(DelegateExecution execution, Context context);

    /**
     * Aggregated the provide variable instances into one variable value.
     *
     * @param execution the delegated execution for which we need to do the aggregation
     * @param instances the variable values that should be aggregated
     * @param context the aggregation context
     * @return the aggregated value
     */
    Object aggregateMulti(DelegateExecution execution, List<? extends VariableInstance> instances, Context context);

    interface Context {

        VariableAggregationDefinition getDefinition();

        String getState();
    }

    interface ContextStates {

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
    }
}
