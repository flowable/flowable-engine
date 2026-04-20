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
package org.flowable.cmmn.engine.impl.scripting;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.scripting.ScriptTraceEnhancer;
import org.flowable.task.api.Task;

/**
 * Enhances script traces with scope information for the cmmn engine.
 *
 * @author Arthur Hupka-Merle
 */
public class CmmnEngineScriptTraceEnhancer implements ScriptTraceEnhancer {

    private static final String EMPTY_INDICATOR = "<empty>";

    @Override
    public void enhanceScriptTrace(ScriptTraceContext scriptTrace) {
        enhanceScriptTrace(scriptTrace, scriptTrace.getVariableContainer());
    }

    protected void enhanceScriptTrace(ScriptTraceContext scriptTrace, VariableContainer container) {
        if (container instanceof Task task) {
            if (ScopeTypes.CMMN.equals((task.getScopeType()))) {
                scriptTrace.addTraceTag("scopeType", ScopeTypes.CMMN);
                CaseDefinition caseDefinition = CaseDefinitionUtil.getCaseDefinition(task.getScopeDefinitionId());
                scriptTrace.addTraceTag("scopeDefinitionKey", caseDefinition.getKey());
                scriptTrace.addTraceTag("scopeDefinitionId", caseDefinition.getId());
                scriptTrace.addTraceTag("subScopeDefinitionKey", task.getTaskDefinitionKey());
            }
        } else if (container instanceof DelegatePlanItemInstance planItemInstance) {
            scriptTrace.addTraceTag("scopeType", ScopeTypes.CMMN);
            CaseDefinition caseDefinition = CaseDefinitionUtil.getCaseDefinition(planItemInstance.getCaseDefinitionId());
            scriptTrace.addTraceTag("scopeDefinitionKey", caseDefinition.getKey());
            scriptTrace.addTraceTag("scopeDefinitionId", planItemInstance.getCaseDefinitionId());
            scriptTrace.addTraceTag("subScopeDefinitionKey", planItemInstance.getPlanItemDefinitionId());
            addTenantId(scriptTrace, planItemInstance.getTenantId());
        } else if (container instanceof CaseInstance caseInstance) {
            scriptTrace.addTraceTag("scopeType", ScopeTypes.CMMN);
            CaseDefinition caseDefinition = CaseDefinitionUtil.getCaseDefinition(caseInstance.getCaseDefinitionId());
            scriptTrace.addTraceTag("scopeDefinitionKey", caseDefinition.getKey());
            scriptTrace.addTraceTag("scopeDefinitionId", caseDefinition.getId());
            addTenantId(scriptTrace, caseInstance.getTenantId());
        }
    }

    protected void addTenantId(ScriptTraceContext scriptTrace, String tenantId) {
        if (tenantId != null && !tenantId.isEmpty()) {
            scriptTrace.addTraceTag("tenantId", tenantId);
        } else {
            scriptTrace.addTraceTag("tenantId", EMPTY_INDICATOR);
        }
    }
}
