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

import java.util.Map;
import java.util.function.Function;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.scripting.Resolver;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class VariableScopeResolver implements Resolver {

    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    protected VariableContainer scopeContainer;
    protected VariableContainer inputVariableContainer;

    protected String variableScopeKey = "execution";

    protected static final Map<String, Function<ProcessEngineConfiguration, ?>> SERVICE_RESOLVERS = Map.of(
            "processEngineConfiguration", Function.identity(),
            "runtimeService", ProcessEngineConfiguration::getRuntimeService,
            "taskService", ProcessEngineConfiguration::getTaskService,
            "repositoryService", ProcessEngineConfiguration::getRepositoryService,
            "managementService", ProcessEngineConfiguration::getManagementService,
            "historyService", ProcessEngineConfiguration::getHistoryService,
            "formService", ProcessEngineConfiguration::getFormService,
            "identityServiceKey", ProcessEngineConfiguration::getIdentityService
    );

    public VariableScopeResolver(ProcessEngineConfigurationImpl processEngineConfiguration, VariableContainer scopeContainer,
            VariableContainer inputVariableContainer) {

        this.processEngineConfiguration = processEngineConfiguration;

        if (scopeContainer == null) {
            throw new FlowableIllegalArgumentException("scopeContainer cannot be null");
        }
        if (scopeContainer instanceof ExecutionEntity) {
            variableScopeKey = "execution";
        } else if (scopeContainer instanceof TaskEntity) {
            variableScopeKey = "task";
        } else {
            throw new FlowableException("unsupported variable scope type: " + scopeContainer.getClass().getName());
        }
        this.scopeContainer = scopeContainer;
        this.inputVariableContainer = inputVariableContainer;
    }

    @Override
    public boolean containsKey(Object key) {
        return variableScopeKey.equals(key) || inputVariableContainer.hasVariable((String) key)
                || SERVICE_RESOLVERS.containsKey(key) && processEngineConfiguration.isServicesEnabledInScripting();
    }

    @Override
    public Object get(Object key) {
        if (variableScopeKey.equals(key)) {
            return scopeContainer;
        } else if (SERVICE_RESOLVERS.containsKey((String) key)) {
            if (processEngineConfiguration.isServicesEnabledInScripting()) {
                return SERVICE_RESOLVERS.get(key).apply(processEngineConfiguration);
            } else {
                throw new FlowableException("The service '" + key + "' is not available in the current context. Please enable services in scripting.");
            }
        }

        return inputVariableContainer.getVariable((String) key);
    }
}
