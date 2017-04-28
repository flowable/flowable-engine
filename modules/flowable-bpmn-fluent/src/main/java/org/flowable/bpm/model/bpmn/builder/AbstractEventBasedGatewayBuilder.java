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
import org.flowable.bpm.model.bpmn.EventBasedGatewayType;
import org.flowable.bpm.model.bpmn.instance.EventBasedGateway;

public class AbstractEventBasedGatewayBuilder<B extends AbstractEventBasedGatewayBuilder<B>>
        extends AbstractGatewayBuilder<B, EventBasedGateway> {

    protected AbstractEventBasedGatewayBuilder(BpmnModelInstance modelInstance, EventBasedGateway element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    /**
     * Sets the build event based gateway to be instantiate.
     *
     * @return the builder object
     */
    public B instantiate() {
        element.setInstantiate(true);
        return myself;
    }

    /**
     * Sets the event gateway type of the build event based gateway.
     *
     * @param eventGatewayType the event gateway type to set
     * @return the builder object
     */
    public B eventGatewayType(EventBasedGatewayType eventGatewayType) {
        element.setEventGatewayType(eventGatewayType);
        return myself;
    }

    @Override
    public B flowableAsync() {
        throw new UnsupportedOperationException("'async' is not supported for 'Event Based Gateway'");
    }

    @Override
    public B flowableAsync(boolean isFlowableAsync) {
        throw new UnsupportedOperationException("'async' is not supported for 'Event Based Gateway'");
    }

}
