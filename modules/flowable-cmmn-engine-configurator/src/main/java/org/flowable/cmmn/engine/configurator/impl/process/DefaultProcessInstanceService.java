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
package org.flowable.cmmn.engine.configurator.impl.process;

import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.engine.impl.process.ProcessInstanceService;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Joram Barrez
 */
public class DefaultProcessInstanceService implements ProcessInstanceService {

    private static final String DELETE_REASON = "deletedFromCmmnCase";
    
    protected RuntimeService processEngineRuntimeService;

    public DefaultProcessInstanceService(RuntimeService runtimeService) {
        this.processEngineRuntimeService = runtimeService;
    }

    @Override
    public String startProcessInstanceByKey(String processDefinitionKey, String tenantId, Map<String, Object> inParametersMap) {
        return startProcessInstanceByKey(processDefinitionKey, null, tenantId, inParametersMap);
    }

    @Override
    public String startProcessInstanceByKey(String processDefinitionKey, String planItemInstanceId, String tenantId, Map<String, Object> inParametersMap) {
        ProcessInstanceBuilder processInstanceBuilder = processEngineRuntimeService.createProcessInstanceBuilder();
        processInstanceBuilder.processDefinitionKey(processDefinitionKey);
        if (tenantId != null) {
            processInstanceBuilder.tenantId(tenantId);
        }

        if (planItemInstanceId != null) {
            processInstanceBuilder.callbackId(planItemInstanceId);
            processInstanceBuilder.callbackType(CallbackTypes.PLAN_ITEM_CHILD_PROCESS);
        }

        for (String target : inParametersMap.keySet()) {
            processInstanceBuilder.variable(target, inParametersMap.get(target));
        }

        ProcessInstance processInstance = processInstanceBuilder.start();
        return processInstance.getId();
    }

    @Override
    public void deleteProcessInstance(String processInstanceId) {
        processEngineRuntimeService.deleteProcessInstance(processInstanceId, DELETE_REASON);
    }

    @Override
    public Map<String, Object> getVariables(String executionId){
       return processEngineRuntimeService.getVariables(executionId);
    }

}
