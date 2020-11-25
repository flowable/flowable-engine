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

import java.util.List;

import org.flowable.cmmn.model.VariableAggregationDefinition;
import org.flowable.variable.api.persistence.entity.VariableInstance;

/**
 * An interface that can be used to aggregate multiple variables into a single one based on an aggregation definition.
 *
 * @author Filip Hrisafov
 */
public interface PlanItemVariableAggregator {

    /**
     * Create a single variable value based on the provided aggregation definition.
     *
     * @param planItemInstance the delegate planItemInstance from where we need to get data from
     * @param context the aggregation context
     * @return the value for the aggregated variable
     */
    Object aggregateSingle(DelegatePlanItemInstance planItemInstance, Context context);

    /**
     * Aggregated the provide variable instances into one variable value.
     *
     * @param planItemInstance the delegated planItemInstance for which we need to do the aggregation
     * @param instances the variable values that should be aggregated
     * @param context the aggregation context
     * @return the aggregated value
     */
    Object aggregateMulti(DelegatePlanItemInstance planItemInstance, List<? extends VariableInstance> instances, Context context);

    interface Context {

        VariableAggregationDefinition getDefinition();

        String getState();
    }

    interface ContextStates {

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
    }
}
