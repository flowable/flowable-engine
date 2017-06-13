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
import org.flowable.bpm.model.bpmn.instance.CallActivity;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableIn;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableOut;

public class AbstractCallActivityBuilder<B extends AbstractCallActivityBuilder<B>>
        extends AbstractActivityBuilder<B, CallActivity> {

    protected AbstractCallActivityBuilder(BpmnModelInstance modelInstance, CallActivity element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    /**
     * Sets the called element
     *
     * @param calledElement the process to call
     * @return the builder object
     */
    public B calledElement(String calledElement) {
        element.setCalledElement(calledElement);
        return myself;
    }

    /**
     * Sets a "flowable in" parameter to pass a variable from the super process instance to the sub process instance
     *
     * @param source the name of variable in the super process instance
     * @param target the name of the variable in the sub process instance
     * @return the builder object
     */
    public B flowableIn(String source, String target) {
        FlowableIn param = modelInstance.newInstance(FlowableIn.class);
        param.setFlowableSource(source);
        param.setFlowableTarget(target);
        addExtensionElement(param);
        return myself;
    }

    /**
     * Sets a "flowable out" parameter to pass a variable from a sub process instance to the super process instance
     *
     * @param source the name of variable in the sub process instance
     * @param target the name of the variable in the super process instance
     * @return the builder object
     */
    public B flowableOut(String source, String target) {
        FlowableOut param = modelInstance.newInstance(FlowableOut.class);
        param.setFlowableSource(source);
        param.setFlowableTarget(target);
        addExtensionElement(param);
        return myself;
    }
}
