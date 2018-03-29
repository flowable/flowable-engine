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
 */
public class RepetitionRule extends PlanItemRule {
    
    public static String DEFAULT_REPETITION_COUNTER_VARIABLE_NAME = "repetitionCounter";
    
    protected String repetitionCounterVariableName;

    public String getRepetitionCounterVariableName() {
        if (repetitionCounterVariableName != null) {
            return repetitionCounterVariableName;
        } else {
            return DEFAULT_REPETITION_COUNTER_VARIABLE_NAME;
        }
    }

    public void setRepetitionCounterVariableName(String repetitionCounterVariableName) {
        this.repetitionCounterVariableName = repetitionCounterVariableName;
    }

    @Override
    public String toString() {
        return "RepetitionRule{" +
                "repetitionCounterVariableName='" + repetitionCounterVariableName + '\'' +
                "} " + super.toString();
    }
}
