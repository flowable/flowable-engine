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
package org.flowable.dmn.engine.impl.hitpolicy;

import org.flowable.dmn.engine.impl.mvel.MvelExecutionContext;
import org.flowable.dmn.model.HitPolicy;
import org.flowable.engine.common.api.FlowableException;

import java.util.List;

/**
 * @author Yvo Swillens
 */
public class HitPolicyPriority extends AbstractHitPolicy implements ComposeRuleOutputBehavior {

    @Override
    public String getHitPolicyName() {
        return HitPolicy.PRIORITY.getValue();
    }

    @Override
    public void composeRuleOutput(int outputNumber, String outputVariableId, Object executionVariable, MvelExecutionContext executionContext) {

        List<Object> outputValues = executionContext.getOutputValues().get(outputNumber);
        Object currentResultVariable = executionContext.getResultVariables().get(outputVariableId);

        if (currentResultVariable == null) {
            executionContext.addOutputResult(outputNumber, outputVariableId, executionVariable);
        } else if (outputValues != null && !outputValues.isEmpty()) {

            if (!outputValues.contains(currentResultVariable)) {
                throw new FlowableException(String.format("HitPolicy %s: output value %s not present in output values of output %d",
                    getHitPolicyName(), currentResultVariable, outputNumber));
            }
            if (!outputValues.contains(executionVariable)) {
                throw new FlowableException(String.format("HitPolicy %s: output value %s not present in output values of output %d",
                    getHitPolicyName(), executionVariable, outputNumber));
            }

            int indexPrevious = outputValues.indexOf(currentResultVariable);
            int indexCurrent = outputValues.indexOf(executionVariable);

            if (indexCurrent < indexPrevious) {
                executionContext.addOutputResult(outputNumber, outputVariableId, executionVariable);
            }
        }
    }
}
