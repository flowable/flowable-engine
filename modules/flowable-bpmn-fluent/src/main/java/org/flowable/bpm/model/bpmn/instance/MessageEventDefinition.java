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
package org.flowable.bpm.model.bpmn.instance;

/**
 * The BPMN messageEventDefinition element.
 */
public interface MessageEventDefinition
        extends EventDefinition {

    Message getMessage();

    void setMessage(Message message);

    Operation getOperation();

    void setOperation(Operation operation);

    /* Flowable extensions */

    String getFlowableClass();

    void setFlowableClass(String flowableClass);

    String getFlowableDelegateExpression();

    void setFlowableDelegateExpression(String flowableExpression);

    String getFlowableExpression();

    void setFlowableExpression(String flowableExpression);

    String getFlowableResultVariable();

    void setFlowableResultVariable(String flowableResultVariable);

    String getFlowableType();

    void setFlowableType(String flowableType);
}
