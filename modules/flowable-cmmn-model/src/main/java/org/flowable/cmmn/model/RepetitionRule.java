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
package org.flowable.cmmn.model;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Joram Barrez
 * @author Micha Kiener
 * @author Filip Hrisafov
 */
public class RepetitionRule extends PlanItemRule {

    public static final String MAX_INSTANCE_COUNT_UNLIMITED_VALUE = "unlimited";
    public static final Integer MAX_INSTANCE_COUNT_UNLIMITED = -1;

    public static final String DEFAULT_REPETITION_COUNTER_VARIABLE_NAME = "repetitionCounter";

    protected String repetitionCounterVariableName;
    protected String collectionVariableName;
    protected String elementVariableName;
    protected String elementIndexVariableName;
    protected Integer maxInstanceCount;

    protected VariableAggregationDefinitions aggregations;

    public String getRepetitionCounterVariableName() {
        if (repetitionCounterVariableName == null) {
            return DEFAULT_REPETITION_COUNTER_VARIABLE_NAME;
        }
        return repetitionCounterVariableName;
    }

    public void setRepetitionCounterVariableName(String repetitionCounterVariableName) {
        this.repetitionCounterVariableName = repetitionCounterVariableName;
    }

    public String getCollectionVariableName() {
        return collectionVariableName;
    }

    public boolean hasCollectionVariable() {
        return StringUtils.isNotEmpty(collectionVariableName);
    }

    public void setCollectionVariableName(String collectionVariableName) {
        this.collectionVariableName = collectionVariableName;
    }

    public String getElementVariableName() {
        return elementVariableName;
    }

    public boolean hasElementVariable() {
        return StringUtils.isNotEmpty(elementVariableName);
    }

    public void setElementVariableName(String elementVariableName) {
        this.elementVariableName = elementVariableName;
    }

    public String getElementIndexVariableName() {
        return elementIndexVariableName;
    }

    public boolean hasElementIndexVariable() {
        return StringUtils.isNotEmpty(elementIndexVariableName);
    }

    public void setElementIndexVariableName(String elementIndexVariableName) {
        this.elementIndexVariableName = elementIndexVariableName;
    }

    public boolean hasLimitedInstanceCount() {
        return maxInstanceCount != null && maxInstanceCount > 0;
    }

    public Integer getMaxInstanceCount() {
        return maxInstanceCount;
    }

    public void setMaxInstanceCount(Integer maxInstanceCount) {
        this.maxInstanceCount = maxInstanceCount;
    }

    public VariableAggregationDefinitions getAggregations() {
        return aggregations;
    }

    public void setAggregations(VariableAggregationDefinitions aggregations) {
        this.aggregations = aggregations;
    }

    public void addAggregation(VariableAggregationDefinition aggregation) {
        if (this.aggregations == null) {
            this.aggregations = new VariableAggregationDefinitions();
        }

        this.aggregations.getAggregations().add(aggregation);
    }

    @Override
    public String toString() {
        return "RepetitionRule{" +
                " maxInstanceCount='" + (hasLimitedInstanceCount() ? maxInstanceCount : "unlimited") + "'" +
                " repetitionCounterVariableName='" + repetitionCounterVariableName + "'" +
                " collectionVariableName='" + collectionVariableName + "'" +
                " elementVariableName='" + elementVariableName + "'" +
                " elementIndexVariableName='" + elementIndexVariableName + "'" +
                " } " + super.toString();
    }
}
