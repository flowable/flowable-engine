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
package org.flowable.engine.impl.delegate;

import java.util.HashMap;
import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ReadOnlyDelegateExecution;

/**
 * @author Filip Hrisafov
 */
public class ReadOnlyDelegateExecutionImpl implements ReadOnlyDelegateExecution {

    protected final String id;
    protected final String processInstanceId;
    protected final String rootProcessInstanceId;
    protected final String eventName;
    protected final String processInstanceBusinessKey;
    protected final String processInstanceBusinessStatus;
    protected final String processDefinitionId;
    protected final String propagatedStageInstanceId;
    protected final String parentId;
    protected final String superExecutionId;
    protected final String currentActivityId;
    protected final String tenantId;
    protected final FlowElement currentFlowElement;
    protected final boolean active;
    protected final boolean ended;
    protected final boolean concurrent;
    protected final boolean processInstanceType;
    protected final boolean scope;
    protected final boolean multiInstanceRoot;
    protected final Map<String, Object> variables;

    public ReadOnlyDelegateExecutionImpl(DelegateExecution execution) {
        this.id = execution.getId();
        this.processInstanceId = execution.getProcessInstanceId();
        this.rootProcessInstanceId = execution.getRootProcessInstanceId();
        this.eventName = execution.getEventName();
        this.processInstanceBusinessKey = execution.getProcessInstanceBusinessKey();
        this.processInstanceBusinessStatus = execution.getProcessInstanceBusinessStatus();
        this.processDefinitionId = execution.getProcessDefinitionId();
        this.propagatedStageInstanceId = execution.getPropagatedStageInstanceId();
        this.parentId = execution.getParentId();
        this.superExecutionId = execution.getSuperExecutionId();
        this.currentActivityId = execution.getCurrentActivityId();
        this.tenantId = execution.getTenantId();
        this.currentFlowElement = execution.getCurrentFlowElement();
        this.active = execution.isActive();
        this.ended = execution.isEnded();
        this.concurrent = execution.isConcurrent();
        this.processInstanceType = execution.isProcessInstanceType();
        this.scope = execution.isScope();
        this.multiInstanceRoot = execution.isMultiInstanceRoot();
        this.variables = new HashMap<>(execution.getVariables());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public String getRootProcessInstanceId() {
        return rootProcessInstanceId;
    }

    @Override
    public String getEventName() {
        return eventName;
    }

    @Override
    public String getProcessInstanceBusinessKey() {
        return processInstanceBusinessKey;
    }

    @Override
    public String getProcessInstanceBusinessStatus() {
        return processInstanceBusinessStatus;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Override
    public String getPropagatedStageInstanceId() {
        return propagatedStageInstanceId;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public String getSuperExecutionId() {
        return superExecutionId;
    }

    @Override
    public String getCurrentActivityId() {
        return currentActivityId;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public FlowElement getCurrentFlowElement() {
        return currentFlowElement;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean isEnded() {
        return ended;
    }

    @Override
    public boolean isConcurrent() {
        return concurrent;
    }

    @Override
    public boolean isProcessInstanceType() {
        return processInstanceType;
    }

    @Override
    public boolean isScope() {
        return scope;
    }

    @Override
    public boolean isMultiInstanceRoot() {
        return multiInstanceRoot;
    }

    @Override
    public Object getVariable(String variableName) {
        return variables.get(variableName);
    }

    @Override
    public boolean hasVariable(String variableName) {
        return variables.containsKey(variableName);
    }
}
