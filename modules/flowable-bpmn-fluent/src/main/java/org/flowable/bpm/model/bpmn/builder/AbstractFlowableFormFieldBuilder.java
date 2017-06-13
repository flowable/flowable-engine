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
import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFormField;

public class AbstractFlowableFormFieldBuilder<P, B extends AbstractFlowableFormFieldBuilder<P, B>>
        extends AbstractBpmnModelElementBuilder<B, FlowableFormField> {

    protected BaseElement parent;

    protected AbstractFlowableFormFieldBuilder(BpmnModelInstance modelInstance, BaseElement parent, FlowableFormField element, Class<?> selfType) {
        super(modelInstance, element, selfType);
        this.parent = parent;
    }

    /**
     * Sets the form field id.
     *
     * @param id the form field id
     * @return the builder object
     */
    public B flowableId(String id) {
        element.setFlowableId(id);
        return myself;
    }

    /**
     * Sets the form field type.
     *
     * @param type the form field type
     * @return the builder object
     */
    public B flowableType(String type) {
        element.setFlowableType(type);
        return myself;
    }

    /**
     * Finishes the building of a form field.
     *
     * @return the parent activity builder
     */
    @SuppressWarnings("unchecked")
    public P flowableFormFieldDone() {
        return (P) parent.builder();
    }
}
