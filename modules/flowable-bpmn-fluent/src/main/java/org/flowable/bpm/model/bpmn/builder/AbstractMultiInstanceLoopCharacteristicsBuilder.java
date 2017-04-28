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
import org.flowable.bpm.model.bpmn.instance.Activity;
import org.flowable.bpm.model.bpmn.instance.CompletionCondition;
import org.flowable.bpm.model.bpmn.instance.LoopCardinality;
import org.flowable.bpm.model.bpmn.instance.MultiInstanceLoopCharacteristics;

public class AbstractMultiInstanceLoopCharacteristicsBuilder<B extends AbstractMultiInstanceLoopCharacteristicsBuilder<B>>
        extends AbstractBaseElementBuilder<B, MultiInstanceLoopCharacteristics> {

    protected AbstractMultiInstanceLoopCharacteristicsBuilder(BpmnModelInstance modelInstance, MultiInstanceLoopCharacteristics element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    /**
     * Sets the multi instance loop characteristics to be sequential.
     *
     * @return the builder object
     */
    public B sequential() {
        element.setSequential(true);
        return myself;
    }

    /**
     * Sets the multi instance loop characteristics to be parallel.
     *
     * @return the builder object
     */
    public B parallel() {
        element.setSequential(false);
        return myself;
    }

    /**
     * Sets the cardinality expression.
     *
     * @param expression the cardinality expression
     * @return the builder object
     */
    public B cardinality(String expression) {
        LoopCardinality cardinality = getCreateSingleChild(LoopCardinality.class);
        cardinality.setTextContent(expression);

        return myself;
    }

    /**
     * Sets the completion condition expression.
     *
     * @param expression the completion condition expression
     * @return the builder object
     */
    public B completionCondition(String expression) {
        CompletionCondition condition = getCreateSingleChild(CompletionCondition.class);
        condition.setTextContent(expression);

        return myself;
    }

    /**
     * Sets the Flowable collection expression.
     *
     * @param expression the collection expression
     * @return the builder object
     */
    public B flowableCollection(String expression) {
        element.setFlowableCollection(expression);

        return myself;
    }

    /**
     * Sets the Flowable element variable name.
     *
     * @param variableName the name of the element variable
     * @return the builder object
     */
    public B flowableElementVariable(String variableName) {
        element.setFlowableElementVariable(variableName);

        return myself;
    }

    /**
     * Finishes the building of a multi instance loop characteristics.
     *
     * @return the parent activity builder
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T extends AbstractActivityBuilder> T multiInstanceDone() {
        return (T) ((Activity) element.getParentElement()).builder();
    }

}
