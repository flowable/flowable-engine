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
import org.flowable.bpm.model.bpmn.instance.Message;
import org.flowable.bpm.model.bpmn.instance.Operation;
import org.flowable.bpm.model.bpmn.instance.SendTask;

public abstract class AbstractSendTaskBuilder<B extends AbstractSendTaskBuilder<B>>
        extends AbstractTaskBuilder<B, SendTask> {

    protected AbstractSendTaskBuilder(BpmnModelInstance modelInstance, SendTask element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    /**
     * Sets the implementation of the send task.
     *
     * @param implementation the implementation to set
     * @return the builder object
     */
    public B implementation(String implementation) {
        element.setImplementation(implementation);
        return myself;
    }

    /**
     * Sets the message of the send task.
     * 
     * @param message the message to set
     * @return the builder object
     */
    public B message(Message message) {
        element.setMessage(message);
        return myself;
    }

    /**
     * Sets the message with the given message name. If already a message with this name exists it will be used, otherwise a new message is created.
     *
     * @param messageName the name of the message
     * @return the builder object
     */
    public B message(String messageName) {
        Message message = findMessageForName(messageName);
        return message(message);
    }

    /**
     * Sets the operation of the send task.
     *
     * @param operation the operation to set
     * @return the builder object
     */
    public B operation(Operation operation) {
        element.setOperation(operation);
        return myself;
    }

    /* Flowable extensions */

    /**
     * Sets the Flowable class attribute.
     *
     * @param delegateClass the class name to set
     * @return the builder object
     */
    public B flowableClass(Class delegateClass) {
        return flowableClass(delegateClass.getName());
    }

    /**
     * Sets the Flowable class attribute.
     *
     * @param fullQualifiedClassName the class name to set
     * @return the builder object
     */
    public B flowableClass(String fullQualifiedClassName) {
        element.setFlowableClass(fullQualifiedClassName);
        return myself;
    }

    /**
     * Sets the Flowable delegateExpression attribute.
     *
     * @param flowableExpression the delegateExpression to set
     * @return the builder object
     */
    public B flowableDelegateExpression(String flowableExpression) {
        element.setFlowableDelegateExpression(flowableExpression);
        return myself;
    }

    /**
     * Sets the Flowable expression attribute.
     *
     * @param flowableExpression the expression to set
     * @return the builder object
     */
    public B flowableExpression(String flowableExpression) {
        element.setFlowableExpression(flowableExpression);
        return myself;
    }

    /**
     * Sets the Flowable resultVariable attribute.
     *
     * @param flowableResultVariable the name of the process variable
     * @return the builder object
     */
    public B flowableResultVariable(String flowableResultVariable) {
        element.setFlowableResultVariable(flowableResultVariable);
        return myself;
    }
}
