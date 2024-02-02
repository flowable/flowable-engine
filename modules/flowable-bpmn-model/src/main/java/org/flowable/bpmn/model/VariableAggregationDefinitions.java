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
package org.flowable.bpmn.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Filip Hrisafov
 */
public class VariableAggregationDefinitions {

    protected Collection<VariableAggregationDefinition> aggregations = new ArrayList<>();

    public Collection<VariableAggregationDefinition> getAggregations() {
        return aggregations;
    }

    public Collection<VariableAggregationDefinition> getOverviewAggregations() {
        return aggregations.stream()
                // An aggregation is an overview aggregation when it is explicitly set and it is not stored as transient
                .filter(agg -> agg.isCreateOverviewVariable() && !agg.isStoreAsTransientVariable())
                .collect(Collectors.toList());
    }

    public void setAggregations(Collection<VariableAggregationDefinition> aggregations) {
        this.aggregations = aggregations;
    }

    @Override
    public VariableAggregationDefinitions clone() {
        VariableAggregationDefinitions aggregations = new VariableAggregationDefinitions();
        aggregations.setValues(this);

        return aggregations;
    }

    public void setValues(VariableAggregationDefinitions otherAggregations) {
        for (VariableAggregationDefinition otherAggregation : otherAggregations.getAggregations()) {
            getAggregations().add(otherAggregation.clone());
        }
    }
}
