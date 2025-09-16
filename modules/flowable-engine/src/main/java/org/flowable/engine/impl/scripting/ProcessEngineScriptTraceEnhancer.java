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
    public void enhanceScriptTrace(ScriptTraceContext context) {
        enhanceScriptTrace(context, context.getVariableContainer());
    }

    protected void enhanceScriptTrace(ScriptTraceContext context, VariableContainer container) {
        if (container instanceof DelegateExecution execution) {
            context.addTraceTag("scopeType", ScopeTypes.BPMN);
            addScopeTags(execution.getProcessDefinitionId(), context);
            context.addTraceTag("subScopeDefinitionKey", execution.getCurrentActivityId());
            addTenantId(context, execution.getTenantId());
        } else if (container instanceof DelegateTask task) {
            if (task.getProcessInstanceId() != null) {
                context.addTraceTag("scopeType", ScopeTypes.BPMN);
                addScopeTags(task.getProcessDefinitionId(), context);
                context.addTraceTag("subScopeDefinitionKey", task.getTaskDefinitionKey());
                addTenantId(context, task.getTenantId());
            }
        }
    }

    protected void addScopeTags(String processDefinitionId, ScriptTraceContext context) {
        ProcessDefinition processDefinition = getProcessDefinition(processDefinitionId);
        if (processDefinition != null) {
            context.addTraceTag("scopeDefinitionKey", processDefinition.getKey());
            context.addTraceTag("scopeDefinitionId", processDefinition.getId());
        }
    }

    protected void addTenantId(ScriptTraceContext context, String tenantId) {
        if (tenantId != null && !tenantId.isEmpty()) {
            context.addTraceTag("tenantId", tenantId);
        } else {
            context.addTraceTag("tenantId", EMPTY_INDICATOR);
        }
    }

    protected ProcessDefinition getProcessDefinition(String processDefinitionId) {
        if (processDefinitionId != null) {
            return ProcessDefinitionUtil.getProcessDefinition(processDefinitionId);
        }
        return null;
    }
}
