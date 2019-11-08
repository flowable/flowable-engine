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

/**
 * @author Joram Barrez
 * @author Micha Kiener
 */
public class RepetitionRule extends PlanItemRule {

    public static final String MAX_INSTANCE_COUNT_ONE = "one";
    public static final String MAX_INSTANCE_COUNT_UNLIMITED = "unlimited";

    public static final String DEFAULT_REPETITION_COUNTER_VARIABLE_NAME = "repetitionCounter";
    public static final String DEFAULT_REPETITION_MAX_INSTANCE_COUNT_VALUE = MAX_INSTANCE_COUNT_ONE;

    protected String repetitionCounterVariableName;
    protected String collectionVariableName;
    protected String elementVariableName;
    protected String elementIndexVariableName;
    protected String maxInstanceCount;

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

    public void setCollectionVariableName(String collectionVariableName) {
        this.collectionVariableName = collectionVariableName;
    }

    public String getElementVariableName() {
        return elementVariableName;
    }

    public void setElementVariableName(String elementVariableName) {
        this.elementVariableName = elementVariableName;
    }

    public String getElementIndexVariableName() {
        return elementIndexVariableName;
    }

    public void setElementIndexVariableName(String elementIndexVariableName) {
        this.elementIndexVariableName = elementIndexVariableName;
    }

    public String getMaxInstanceCount() {
        if (maxInstanceCount == null) {
            return DEFAULT_REPETITION_MAX_INSTANCE_COUNT_VALUE;
        }
        return maxInstanceCount;
    }

    public void setMaxInstanceCount(String maxInstanceCount) {
        this.maxInstanceCount = maxInstanceCount;
    }

    @Override
    public String toString() {
        return "RepetitionRule{" +
                " maxInstanceCount='" + maxInstanceCount + "'" +
                " repetitionCounterVariableName='" + repetitionCounterVariableName + "'" +
                " collectionVariableName='" + collectionVariableName + "'" +
                " elementVariableName='" + elementVariableName + "'" +
                " elementIndexVariableName='" + elementIndexVariableName + "'" +
                " } " + super.toString();
    }
}
