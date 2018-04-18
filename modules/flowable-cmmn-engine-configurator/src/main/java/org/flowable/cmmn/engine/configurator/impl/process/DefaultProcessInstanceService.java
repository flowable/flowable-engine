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

import org.flowable.cmmn.api.PlanItemInstanceCallbackType;
import org.flowable.cmmn.engine.impl.process.ProcessInstanceService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;

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
    public String startProcessInstanceByKey(String processDefinitionKey, String tenantId) {
        return startProcessInstanceByKey(processDefinitionKey, null, tenantId);
    }
    
    @Override
    public String startProcessInstanceByKey(String processDefinitionKey, String planItemInstanceId, String tenantId) {
        ProcessInstanceBuilder processInstanceBuilder = processEngineRuntimeService.createProcessInstanceBuilder();
        processInstanceBuilder.processDefinitionKey(processDefinitionKey);
        if (tenantId != null) {
            processInstanceBuilder.tenantId(tenantId);
        }
        
        if (planItemInstanceId != null) {
            processInstanceBuilder.callbackId(planItemInstanceId);
            processInstanceBuilder.callbackType(PlanItemInstanceCallbackType.CHILD_PROCESS);
        }
        
        ProcessInstance processInstance = processInstanceBuilder.start();
        return processInstance.getId();
    }
    
    @Override
    public void deleteProcessInstance(String processInstanceId) {
        processEngineRuntimeService.deleteProcessInstance(processInstanceId, DELETE_REASON);
    }

}
