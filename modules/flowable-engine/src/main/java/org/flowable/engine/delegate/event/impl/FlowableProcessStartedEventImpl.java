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
package org.flowable.engine.delegate.event.impl;

import java.util.Map;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.FlowableProcessStartedEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

/**
 * An {@link org.flowable.engine.delegate.event.FlowableCancelledEvent} implementation.
 *
 * @author martin.grofcik
 */
public class FlowableProcessStartedEventImpl extends FlowableEntityWithVariablesEventImpl implements FlowableProcessStartedEvent {

    protected final String nestedProcessInstanceId;

    protected final String nestedProcessDefinitionId;

    public FlowableProcessStartedEventImpl(final Object entity, final Map variables, final boolean localScope) {
        super(entity, variables, localScope, FlowableEngineEventType.PROCESS_STARTED);
        if (entity instanceof ExecutionEntity) {
            ExecutionEntity executionEntity = (ExecutionEntity) entity;
            if (!executionEntity.isProcessInstanceType()) {
                executionEntity = executionEntity.getParent();
            }

            final ExecutionEntity superExecution = executionEntity.getSuperExecution();
            if (superExecution != null) {
                this.nestedProcessDefinitionId = superExecution.getProcessDefinitionId();
                this.nestedProcessInstanceId = superExecution.getProcessInstanceId();
            } else {
                this.nestedProcessDefinitionId = null;
                this.nestedProcessInstanceId = null;
            }

        } else {
            this.nestedProcessDefinitionId = null;
            this.nestedProcessInstanceId = null;
        }
    }

    @Override
    public String getNestedProcessInstanceId() {
        return this.nestedProcessInstanceId;
    }

    @Override
    public String getNestedProcessDefinitionId() {
        return this.nestedProcessDefinitionId;
    }

}
