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
import org.flowable.bpm.model.bpmn.instance.EndEvent;
import org.flowable.bpm.model.bpmn.instance.ErrorEventDefinition;

public abstract class AbstractEndEventBuilder<B extends AbstractEndEventBuilder<B>>
        extends AbstractThrowEventBuilder<B, EndEvent> {

    protected AbstractEndEventBuilder(BpmnModelInstance modelInstance, EndEvent element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    /**
     * Sets an error definition for the given error code. If already an error with this code exists it will be used, otherwise a new error is created.
     *
     * @param errorCode the code of the error
     * @return the builder object
     */
    public B error(String errorCode) {
        ErrorEventDefinition errorEventDefinition = createErrorEventDefinition(errorCode);
        element.getEventDefinitions().add(errorEventDefinition);

        return myself;
    }
}
