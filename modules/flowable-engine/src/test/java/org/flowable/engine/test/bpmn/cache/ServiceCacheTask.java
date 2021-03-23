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
package org.flowable.engine.test.bpmn.cache;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;

public class ServiceCacheTask implements JavaDelegate {
    
    public static String processInstanceId;
    public static String executionId;
    public static String historicProcessInstanceId;
    public static String historicProcessInstanceDefinitionKey;

    public static void reset() {
        processInstanceId = null;
        executionId = null;
        historicProcessInstanceId = null;
        historicProcessInstanceDefinitionKey = null;
    }

    @Override
    public void execute(DelegateExecution execution) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(execution.getProcessInstanceId()).singleResult();
        if (processInstance != null && processInstance.getId().equals(execution.getProcessInstanceId())) {
            processInstanceId = processInstance.getId();
        }
        
        Execution queryExecution = runtimeService.createExecutionQuery().executionId(execution.getId()).singleResult();
        if (queryExecution != null && execution.getId().equals(queryExecution.getId())) {
            executionId = queryExecution.getId();
        }
        
        HistoryService historyService = processEngineConfiguration.getHistoryService();
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(execution.getProcessInstanceId()).singleResult();
        if (historicProcessInstance != null && historicProcessInstance.getId().equals(execution.getProcessInstanceId())) {
            historicProcessInstanceId = historicProcessInstance.getId();
            historicProcessInstanceDefinitionKey = historicProcessInstance.getProcessDefinitionKey();
        }
    }

}
