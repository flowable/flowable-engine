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
import org.flowable.bpm.model.bpmn.instance.FlowElement;

public abstract class AbstractFlowElementBuilder<B extends AbstractFlowElementBuilder<B, E>, E extends FlowElement>
        extends AbstractBaseElementBuilder<B, E> {

    protected AbstractFlowElementBuilder(BpmnModelInstance modelInstance, E element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    /**
     * Sets the element name attribute.
     *
     * @param name the name to set
     * @return the builder object
     */
    public B name(String name) {
        element.setName(name);
        return myself;
    }
}
