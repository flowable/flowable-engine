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
import org.flowable.bpm.model.bpmn.instance.BoundaryEvent;
import org.flowable.bpm.model.bpmn.instance.MultiInstanceLoopCharacteristics;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableInputOutput;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableInputParameter;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableOutputParameter;

public abstract class AbstractActivityBuilder<B extends AbstractActivityBuilder<B, E>, E extends Activity>
        extends AbstractFlowNodeBuilder<B, E> {

    protected AbstractActivityBuilder(BpmnModelInstance modelInstance, E element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    public BoundaryEventBuilder boundaryEvent() {
        return boundaryEvent(null);
    }

    public BoundaryEventBuilder boundaryEvent(String id) {
        BoundaryEvent boundaryEvent = createSibling(BoundaryEvent.class, id);
        boundaryEvent.setAttachedTo(element);

        return boundaryEvent.builder();
    }

    public MultiInstanceLoopCharacteristicsBuilder multiInstance() {
        MultiInstanceLoopCharacteristics miCharacteristics = createChild(MultiInstanceLoopCharacteristics.class);

        return miCharacteristics.builder();
    }

    /**
     * Creates a new Flowable input parameter extension element with the given name and value.
     *
     * @param name the name of the input parameter
     * @param value the value of the input parameter
     * @return the builder object
     */
    public B flowableInputParameter(String name, String value) {
        FlowableInputOutput flowableInputOutput = getCreateSingleExtensionElement(FlowableInputOutput.class);

        FlowableInputParameter flowableInputParameter = createChild(flowableInputOutput, FlowableInputParameter.class);
        flowableInputParameter.setFlowableName(name);
        flowableInputParameter.setTextContent(value);

        return myself;
    }

    /**
     * Creates a new Flowable output parameter extension element with the given name and value.
     *
     * @param name the name of the output parameter
     * @param value the value of the output parameter
     * @return the builder object
     */
    public B flowableOutputParameter(String name, String value) {
        FlowableInputOutput flowableInputOutput = getCreateSingleExtensionElement(FlowableInputOutput.class);

        FlowableOutputParameter flowableOutputParameter = createChild(flowableInputOutput, FlowableOutputParameter.class);
        flowableOutputParameter.setFlowableName(name);
        flowableOutputParameter.setTextContent(value);

        return myself;
    }
}
