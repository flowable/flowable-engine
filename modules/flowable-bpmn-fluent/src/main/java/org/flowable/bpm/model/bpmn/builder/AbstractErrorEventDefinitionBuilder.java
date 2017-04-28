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
import org.flowable.bpm.model.bpmn.instance.ErrorEventDefinition;
import org.flowable.bpm.model.bpmn.instance.Event;

public abstract class AbstractErrorEventDefinitionBuilder<B extends AbstractErrorEventDefinitionBuilder<B>>
        extends AbstractRootElementBuilder<B, ErrorEventDefinition> {

    public AbstractErrorEventDefinitionBuilder(BpmnModelInstance modelInstance, ErrorEventDefinition element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    @Override
    public B id(String identifier) {
        return super.id(identifier);
    }

    /**
     * Sets the error attribute with errorCode.
     */
    public B error(String errorCode) {
        element.setError(findErrorForNameAndCode(errorCode));
        return myself;
    }

    /**
     * Finishes the building of a error event definition.
     *
     * @param <T>
     * @return the parent event builder
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T extends AbstractFlowNodeBuilder> T errorEventDefinitionDone() {
        return (T) ((Event) element.getParentElement()).builder();
    }
}
