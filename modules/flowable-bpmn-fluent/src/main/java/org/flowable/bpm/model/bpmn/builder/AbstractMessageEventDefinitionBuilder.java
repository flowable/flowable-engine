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
import org.flowable.bpm.model.bpmn.instance.Event;
import org.flowable.bpm.model.bpmn.instance.MessageEventDefinition;

public abstract class AbstractMessageEventDefinitionBuilder<B extends AbstractMessageEventDefinitionBuilder<B>>
        extends AbstractRootElementBuilder<B, MessageEventDefinition> {

    public AbstractMessageEventDefinitionBuilder(BpmnModelInstance modelInstance, MessageEventDefinition element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    @Override
    public B id(String identifier) {
        return super.id(identifier);
    }

    /**
     * Sets the message attribute.
     *
     * @param message the message for the message event definition
     * @return the builder object
     */
    public B message(String message) {
        element.setMessage(findMessageForName(message));
        return myself;
    }

    /**
     * Sets the Flowable type attribute.
     *
     * @param flowableType the type of the service task
     * @return the builder object
     */
    public B flowableType(String flowableType) {
        element.setFlowableType(flowableType);
        return myself;
    }

    /**
     * Finishes the building of a message event definition.
     *
     * @param <T>
     * @return the parent event builder
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T extends AbstractFlowNodeBuilder> T messageEventDefinitionDone() {
        return (T) ((Event) element.getParentElement()).builder();
    }
}
