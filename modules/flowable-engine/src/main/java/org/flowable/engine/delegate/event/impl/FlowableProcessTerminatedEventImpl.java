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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.FlowableProcessTerminatedEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

/**
 * An {@link FlowableProcessTerminatedEvent} implementation.
 *
 * @author martin.grofcik
 */
public class FlowableProcessTerminatedEventImpl extends FlowableEntityEventImpl implements FlowableProcessTerminatedEvent {

    protected Object cause;

    public FlowableProcessTerminatedEventImpl(ExecutionEntity execution, Object cause) {
        super(execution, FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
        if (!execution.isProcessInstanceType()) {
            throw new FlowableException("Execution '"+ execution +"' is not a processInstance");
        }
        
        this.executionId = execution.getId();
        this.processInstanceId = execution.getProcessInstanceId();
        this.processDefinitionId = execution.getProcessDefinitionId();
        this.cause = cause;
    }

    @Override
    public Object getCause() {
        return cause;
    }

}
