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

import java.util.Arrays;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.scripting.Resolver;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class VariableScopeResolver implements Resolver {

    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    protected VariableScope variableScope;

    protected String variableScopeKey = "execution";

    protected static final String processEngineConfigurationKey = "processEngineConfiguration";
    protected static final String runtimeServiceKey = "runtimeService";
    protected static final String taskServiceKey = "taskService";
    protected static final String repositoryServiceKey = "repositoryService";
    protected static final String managementServiceKey = "managementService";
    protected static final String historyServiceKey = "historyService";
    protected static final String formServiceKey = "formService";
    protected static final String identityServiceKey = "identityServiceKey";

    protected static final List<String> KEYS = Arrays.asList(
            processEngineConfigurationKey, runtimeServiceKey, taskServiceKey,
            repositoryServiceKey, managementServiceKey, historyServiceKey, formServiceKey, identityServiceKey);

    public VariableScopeResolver(ProcessEngineConfigurationImpl processEngineConfiguration, VariableScope variableScope) {

        this.processEngineConfiguration = processEngineConfiguration;

        if (variableScope == null) {
            throw new FlowableIllegalArgumentException("variableScope cannot be null");
        }
        if (variableScope instanceof ExecutionEntity) {
            variableScopeKey = "execution";
        } else if (variableScope instanceof TaskEntity) {
            variableScopeKey = "task";
        } else {
            throw new FlowableException("unsupported variable scope type: " + variableScope.getClass().getName());
        }
        this.variableScope = variableScope;
    }

    @Override
    public boolean containsKey(Object key) {
        return variableScopeKey.equals(key) || KEYS.contains(key) || variableScope.hasVariable((String) key);
    }

    @Override
    public Object get(Object key) {
        if (variableScopeKey.equals(key)) {
            return variableScope;
        } else if (processEngineConfigurationKey.equals(key)) {
            return processEngineConfiguration;
        } else if (runtimeServiceKey.equals(key)) {
            return processEngineConfiguration.getRuntimeService();
        } else if (taskServiceKey.equals(key)) {
            return processEngineConfiguration.getTaskService();
        } else if (repositoryServiceKey.equals(key)) {
            return processEngineConfiguration.getRepositoryService();
        } else if (managementServiceKey.equals(key)) {
            return processEngineConfiguration.getManagementService();
        } else if (formServiceKey.equals(key)) {
            return processEngineConfiguration.getFormService();
        } else if (identityServiceKey.equals(key)) {
            return processEngineConfiguration.getIdentityService();
        }

        return variableScope.getVariable((String) key);
    }
}
