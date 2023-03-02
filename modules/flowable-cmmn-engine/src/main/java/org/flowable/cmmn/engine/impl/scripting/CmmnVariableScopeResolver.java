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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.scripting.Resolver;

/**
 *
 * @author Dennis Federico
 * @author Joram Barrez
 */
public class CmmnVariableScopeResolver implements Resolver {

    protected static final String ENGINE_CONFIG_KEY = "engineConfiguration";
    protected static final String CMMN_ENGINE_CONFIG_KEY = "cmmnEngineConfiguration";

    protected static final String RUNTIME__SERVICE_KEY = "runtimeService";
    protected static final String CMMN_RUNTIME__SERVICE_KEY = "cmmnRuntimeService";

    protected static final String HISTORY_SERVICE_KEY = "historyService";
    protected static final String CMMN_HISTORY_SERVICE_KEY = "cmmnHistoryService";

    protected static final String MANAGEMENT_SERVICE_KEY = "managementService";
    protected static final String CMMN_MANAGEMENT_SERVICE_KEY = "cmmnManagementService";

    protected static final String TASK_SERVICE_KEY = "taskService";
    protected static final String CMMN_TASK_SERVICE_KEY = "cmmnTaskService";

    protected static final String CASE_INSTANCE_KEY = "caseInstance";
    protected static final String PLAN_ITEM_INSTANCE_KEY = "planItemInstance";
    protected static final String TASK_KEY = "task";

    protected static final Set<String> KEYS = new HashSet<>(Arrays.asList(
        ENGINE_CONFIG_KEY, CMMN_ENGINE_CONFIG_KEY,
        RUNTIME__SERVICE_KEY, CMMN_RUNTIME__SERVICE_KEY,
        HISTORY_SERVICE_KEY, CMMN_HISTORY_SERVICE_KEY,
        MANAGEMENT_SERVICE_KEY, CMMN_MANAGEMENT_SERVICE_KEY,
        TASK_SERVICE_KEY, CMMN_TASK_SERVICE_KEY,
        CASE_INSTANCE_KEY,
        PLAN_ITEM_INSTANCE_KEY,
        TASK_KEY
    ));

    protected CmmnEngineConfiguration engineConfiguration;
    protected VariableContainer variableContainer;

    public CmmnVariableScopeResolver(CmmnEngineConfiguration engineConfiguration, VariableContainer variableContainer) {
        if (variableContainer == null) {
            throw new FlowableIllegalArgumentException("variableContainer cannot be null");
        }
        this.variableContainer = variableContainer;
        this.engineConfiguration = engineConfiguration;
    }

    @Override
    public boolean containsKey(Object key) {
        return variableContainer.hasVariable((String) key) || KEYS.contains(key);
    }

    @Override
    public Object get(Object key) {
        if (ENGINE_CONFIG_KEY.equals(key) || CMMN_ENGINE_CONFIG_KEY.equals(key)) {
            return engineConfiguration;

        } else if (RUNTIME__SERVICE_KEY.equals(key) || CMMN_RUNTIME__SERVICE_KEY.equals(key)) {
            return engineConfiguration.getCmmnRuntimeService();

        } else if (HISTORY_SERVICE_KEY.equals(key) || CMMN_HISTORY_SERVICE_KEY.equals(key)) {
            return engineConfiguration.getCmmnHistoryService();

        } else if (MANAGEMENT_SERVICE_KEY.equals(key) || CMMN_MANAGEMENT_SERVICE_KEY.equals(key)) {
            return engineConfiguration.getCmmnManagementService();

        } else if (TASK_SERVICE_KEY.equals(key) || CMMN_TASK_SERVICE_KEY.equals(key)) {
            return engineConfiguration.getCmmnTaskService();

        } else if (CASE_INSTANCE_KEY.equals(key) || PLAN_ITEM_INSTANCE_KEY.equals(key) || TASK_KEY.equals(key)) { // task/planItemInstance/caseInstance
            return variableContainer;

        } else {
            return variableContainer.getVariable((String) key);

        }
    }
}
