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
import org.flowable.bpm.model.bpmn.ProcessType;
import org.flowable.bpm.model.bpmn.instance.Process;

public abstract class AbstractProcessBuilder<B extends AbstractProcessBuilder<B>>
        extends AbstractCallableElementBuilder<B, Process> {

    protected AbstractProcessBuilder(BpmnModelInstance modelInstance, Process element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    /**
     * Sets the process type for this.
     *
     * @param processType the process type to set
     * @return the builder object
     */
    public B processType(ProcessType processType) {
        element.setProcessType(processType);
        return myself;
    }

    /**
     * Sets this closed.
     *
     * @return the builder object
     */
    public B closed() {
        element.setClosed(true);
        return myself;
    }

    /**
     * Sets this executable.
     *
     * @return the builder object
     */
    public B executable() {
        element.setExecutable(true);
        return myself;
    }
}
