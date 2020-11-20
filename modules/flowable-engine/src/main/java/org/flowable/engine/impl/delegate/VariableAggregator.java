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
     * @param aggregation the aggregation definition
     * @return the value for the aggregated variable
     */
    Object aggregateSingle(DelegateExecution execution, VariableAggregationDefinition aggregation);

    /**
     * Aggregated the provide variable instances into one variable value.
     *
     * @param execution the delegated execution for which we need to do the aggregation
     * @param instances the variable values that should be aggregated
     * @param aggregation the aggregation definition
     * @return the aggregated value
     */
    Object aggregateMulti(DelegateExecution execution, List<? extends VariableInstance> instances, VariableAggregationDefinition aggregation);
}
