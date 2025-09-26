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

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.scripting.Resolver;
import org.flowable.task.api.Task;

/**
 *
 * @author Dennis Federico
 * @author Joram Barrez
 */
public class CmmnVariableScopeResolver implements Resolver {

    protected static final String CASE_INSTANCE_KEY = "caseInstance";
    protected static final String PLAN_ITEM_INSTANCE_KEY = "planItemInstance";
    protected static final String TASK_KEY = "task";

    protected static final Map<String, Function<CmmnEngineConfiguration, ?>> SERVICE_RESOLVERS = Map.of(
            "engineConfiguration", Function.identity(),
            "cmmnEngineConfiguration", Function.identity(),
            "runtimeService", CmmnEngineConfiguration::getCmmnRuntimeService,
            "cmmnRuntimeService", CmmnEngineConfiguration::getCmmnRuntimeService,
            "historyService", CmmnEngineConfiguration::getCmmnHistoryService,
            "cmmnHistoryService", CmmnEngineConfiguration::getCmmnHistoryService,
            "managementService", CmmnEngineConfiguration::getCmmnManagementService,
            "cmmnManagementService", CmmnEngineConfiguration::getCmmnManagementService,
            "taskService", CmmnEngineConfiguration::getCmmnTaskService,
            "cmmnTaskService", CmmnEngineConfiguration::getCmmnTaskService
    );

    protected static final Set<String> KEYS = Set.of(
        CASE_INSTANCE_KEY,
        PLAN_ITEM_INSTANCE_KEY,
        TASK_KEY
    );

    protected CmmnEngineConfiguration engineConfiguration;
    protected VariableContainer scopeContainer;
    protected VariableContainer inputVariableContainer;

    public CmmnVariableScopeResolver(CmmnEngineConfiguration engineConfiguration, VariableContainer scopeContainer,
            VariableContainer inputVariableContainer) {
        if (scopeContainer == null) {
            throw new FlowableIllegalArgumentException("scopeContainer cannot be null");
        }
        this.scopeContainer = scopeContainer;
        this.inputVariableContainer = inputVariableContainer;
        this.engineConfiguration = engineConfiguration;
    }

    @Override
    public boolean containsKey(Object key) {
        return inputVariableContainer.hasVariable((String) key) || KEYS.contains(key)
                || SERVICE_RESOLVERS.containsKey(key) && engineConfiguration.isServicesEnabledInScripting();
    }

    @Override
    public Object get(Object key) {
        if (SERVICE_RESOLVERS.containsKey((String) key)) {
            if (engineConfiguration.isServicesEnabledInScripting()) {
                return SERVICE_RESOLVERS.get(key).apply(engineConfiguration);
            } else {
                throw new FlowableException("The service '" + key + "' is not available in the current context. Please enable services in scripting.");
            }
        } else if (CASE_INSTANCE_KEY.equals(key)) {
            if (scopeContainer instanceof CaseInstance) {
                return scopeContainer;

            } else if (scopeContainer instanceof PlanItemInstance planItemInstance) {
                if (StringUtils.isNotEmpty(planItemInstance.getCaseInstanceId())) {
                    return CommandContextUtil.getCaseInstanceEntityManager().findById(planItemInstance.getCaseInstanceId());
                }

            } else if (scopeContainer instanceof Task task) {
                if (StringUtils.isNotEmpty(task.getScopeId()) && ScopeTypes.CMMN.equals(task.getScopeType())) {
                    return CommandContextUtil.getCaseInstanceEntityManager().findById(task.getScopeId());
                }

            }

            throw new FlowableException("Unsupported variableContainer for key '" + CASE_INSTANCE_KEY + "': " + scopeContainer.getClass().getName());

        } else if (PLAN_ITEM_INSTANCE_KEY.equals(key)) {
            if (scopeContainer instanceof PlanItemInstance) {
                return scopeContainer;

            }  else if (scopeContainer instanceof Task task) {
                if (StringUtils.isNotEmpty(task.getSubScopeId()) && ScopeTypes.CMMN.equals(task.getScopeType())) {
                    return CommandContextUtil.getPlanItemInstanceEntityManager().findById(task.getSubScopeId());
                }

            }

            throw new FlowableException("Unsupported variableContainer for key '" + PLAN_ITEM_INSTANCE_KEY + "': " + scopeContainer.getClass().getName());

        } else if (TASK_KEY.equals(key)) {
            if (scopeContainer instanceof Task) {
                return scopeContainer;

            } else  if (scopeContainer instanceof PlanItemInstance planItemInstance) {
                return CommandContextUtil.getTaskService().findTasksBySubScopeIdScopeType(planItemInstance.getId(), ScopeTypes.CMMN);

            } else {
                throw new FlowableException("Unsupported variableContainer for key '" + TASK_KEY + "': " + scopeContainer.getClass().getName());

            }

        } else {
            return inputVariableContainer.getVariable((String) key);

        }
    }
}
