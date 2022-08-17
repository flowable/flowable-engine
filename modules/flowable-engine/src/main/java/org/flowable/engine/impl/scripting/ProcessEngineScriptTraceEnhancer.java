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
package org.flowable.engine.impl.scripting;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.scripting.ScriptTraceEnhancer;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.service.delegate.DelegateTask;

/**
 * Enhances script traces with scope information for the process engine.
 *
 * @author Arthur Hupka-Merle
 */
public class ProcessEngineScriptTraceEnhancer implements ScriptTraceEnhancer {

    private static final String EMPTY_INDICATOR = "<empty>";

    @Override
    public void enhanceScriptTrace(ScriptTraceContext scriptTrace) {
        VariableContainer container = scriptTrace.getRequest().getVariableContainer();
        if (container instanceof DelegateExecution) {
            scriptTrace.addTraceTag("scopeType", ScopeTypes.BPMN);
            DelegateExecution execution = (DelegateExecution) scriptTrace.getRequest().getVariableContainer();
            addScopeTags(execution.getProcessDefinitionId(), scriptTrace);
            scriptTrace.addTraceTag("subScopeDefinitionKey", execution.getCurrentActivityId());
            addTenantId(scriptTrace, execution.getTenantId());
        } else if (container instanceof DelegateTask) {
            DelegateTask task = (DelegateTask) scriptTrace.getRequest().getVariableContainer();
            if (task.getProcessInstanceId() != null) {
                scriptTrace.addTraceTag("scopeType", ScopeTypes.BPMN);
                addScopeTags(task.getProcessDefinitionId(), scriptTrace);
                scriptTrace.addTraceTag("subScopeDefinitionKey", task.getTaskDefinitionKey());
                addTenantId(scriptTrace, task.getTenantId());
            }
        }
    }

    protected void addScopeTags(String processDefinitionId, ScriptTraceContext scriptTrace) {
        ProcessDefinition processDefinition = getProcessDefinition(processDefinitionId);
        if (processDefinition != null) {
            scriptTrace.addTraceTag("scopeDefinitionKey", processDefinition.getKey());
            scriptTrace.addTraceTag("scopeDefinitionId", processDefinition.getId());
        }
    }

    protected void addTenantId(ScriptTraceContext scriptTrace, String tenantId) {
        if (tenantId != null && !tenantId.isEmpty()) {
            scriptTrace.addTraceTag("tenantId", tenantId);
        } else {
            scriptTrace.addTraceTag("tenantId", EMPTY_INDICATOR);
        }
    }

    protected ProcessDefinition getProcessDefinition(String processDefinitionId) {
        if (processDefinitionId != null) {
            return ProcessDefinitionUtil.getProcessDefinition(processDefinitionId);
        }
        return null;
    }
}
