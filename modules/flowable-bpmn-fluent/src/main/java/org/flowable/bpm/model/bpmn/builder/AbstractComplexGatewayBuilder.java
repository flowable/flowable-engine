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
package org.flowable.bpm.model.bpmn.builder;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.instance.ActivationCondition;
import org.flowable.bpm.model.bpmn.instance.ComplexGateway;
import org.flowable.bpm.model.bpmn.instance.SequenceFlow;

public class AbstractComplexGatewayBuilder<B extends AbstractComplexGatewayBuilder<B>>
        extends AbstractGatewayBuilder<B, ComplexGateway> {

    protected AbstractComplexGatewayBuilder(BpmnModelInstance modelInstance, ComplexGateway element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    /**
     * Sets the default sequence flow for the build complex gateway.
     *
     * @param sequenceFlow the default sequence flow to set
     * @return the builder object
     */
    public B defaultFlow(SequenceFlow sequenceFlow) {
        element.setDefault(sequenceFlow);
        return myself;
    }

    /**
     * Sets the activation condition expression for the build complex gateway
     *
     * @param conditionExpression the activation condition expression to set
     * @return the builder object
     */
    public B activationCondition(String conditionExpression) {
        ActivationCondition activationCondition = createInstance(ActivationCondition.class);
        activationCondition.setTextContent(conditionExpression);
        element.setActivationCondition(activationCondition);
        return myself;
    }

}
