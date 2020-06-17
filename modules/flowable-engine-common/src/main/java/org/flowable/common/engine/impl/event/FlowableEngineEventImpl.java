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
import org.flowable.common.engine.api.scope.ScopeTypes;

/**
 * Base class for all {@link FlowableEngineEvent} implementations.
 *
 * @author Frederik Heremans
 */
public class FlowableEngineEventImpl extends FlowableEventImpl implements FlowableEngineEvent {

    protected String scopeType;
    protected String scopeId;
    protected String subScopeId;
    protected String scopeDefinitionId;

    /**
     * Creates a new event implementation, not part of an execution context.
     */
    public FlowableEngineEventImpl(FlowableEngineEventType type) {
        super(type);
    }

    /**
     * Creates a new event implementation, part of an execution context.
     */
    public FlowableEngineEventImpl(FlowableEngineEventType type, String executionId, String processInstanceId, String processDefinitionId) {
        this(type, ScopeTypes.BPMN, processInstanceId, executionId, processDefinitionId);
    }

    /**
     * Creates a new event implementation, part of an execution context.
     */
    public FlowableEngineEventImpl(FlowableEngineEventType type, String scopeType, String scopeId, String subScopeId, String scopeDefinitionId) {
        super(type);
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.subScopeId = subScopeId;
        this.scopeDefinitionId = scopeDefinitionId;
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
        return ScopeTypes.BPMN.equals(scopeType) ? subScopeId : null;
    }

    public void setExecutionId(String executionId) {
        setScopeType(ScopeTypes.BPMN);
        setSubScopeId(executionId);
    }

    @Override
    public String getProcessDefinitionId() {
        return ScopeTypes.BPMN.equals(scopeType) ? scopeDefinitionId : null;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        setScopeType(ScopeTypes.BPMN);
        setScopeDefinitionId(processDefinitionId);
    }

    @Override
    public String getProcessInstanceId() {
        return ScopeTypes.BPMN.equals(scopeType) ? scopeId : null;
    }

    public void setProcessInstanceId(String processInstanceId) {
        setScopeType(ScopeTypes.BPMN);
        setScopeId(processInstanceId);
    }

    @Override
    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    @Override
    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    @Override
    public String getSubScopeId() {
        return subScopeId;
    }

    public void setSubScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
    }

    @Override
    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }
}
