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
package org.flowable.common.engine.impl.event;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;

/**
 * Base class for all {@link FlowableEngineEvent} implementations.
 *
 * @author Frederik Heremans
 */
public class FlowableEngineEventImpl extends FlowableEventImpl implements FlowableEngineEvent {

    protected String executionId;
    protected String processInstanceId;
    protected String processDefinitionId;

    /**
     * Creates a new event implementation, not part of an execution context.
     */
    public FlowableEngineEventImpl(FlowableEngineEventType type) {
        this(type, null, null, null);
    }

    /**
     * Creates a new event implementation, part of an execution context.
     */
    public FlowableEngineEventImpl(FlowableEngineEventType type, String executionId, String processInstanceId, String processDefinitionId) {
        super(type);
        this.executionId = executionId;
        this.processInstanceId = processInstanceId;
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public FlowableEngineEventType getType() {
        return (FlowableEngineEventType) super.getType();
    }

    public void setType(FlowableEngineEventType type) {
        this.type = type;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }
}
