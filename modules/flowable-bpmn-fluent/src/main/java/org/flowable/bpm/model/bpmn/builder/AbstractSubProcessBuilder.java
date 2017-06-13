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
import org.flowable.bpm.model.bpmn.instance.SubProcess;

public class AbstractSubProcessBuilder<B extends AbstractSubProcessBuilder<B>>
        extends AbstractActivityBuilder<B, SubProcess> {

    protected AbstractSubProcessBuilder(BpmnModelInstance modelInstance, SubProcess element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    public EmbeddedSubProcessBuilder embeddedSubProcess() {
        return new EmbeddedSubProcessBuilder(this);
    }

    /**
     * Sets the sub process to be triggered by an event.
     *
     * @return the builder object
     */
    public B triggerByEvent() {
        element.setTriggeredByEvent(true);
        return myself;
    }
}
