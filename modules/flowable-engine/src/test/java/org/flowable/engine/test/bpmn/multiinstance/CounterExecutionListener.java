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

package org.flowable.engine.test.bpmn.multiinstance;

import java.util.List;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CounterExecutionListener implements ExecutionListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CounterExecutionListener.class);
    
    private static final long serialVersionUID = 1L;

    @Override
    public void notify(DelegateExecution execution) {
        Object nrOfInstancesObj = execution.getVariable("nrOfInstances");
        Object nrOfCompletedInstancesObj = execution.getVariable("nrOfCompletedInstances");
        String nrOfInstances = null == nrOfInstancesObj ? null : nrOfInstancesObj.toString();
        String nrOfCompletedInstances = null == nrOfCompletedInstancesObj ? null : nrOfCompletedInstancesObj.toString();
        LOGGER.debug("Invoke into executionEnd method and execution is {}", execution.getVariables());
        
        if (nrOfCompletedInstances != null && nrOfInstances.equals(nrOfCompletedInstances)) {
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
            
            if (processEngineConfiguration.getHistoryManager().isHistoryEnabled()) {
                List<HistoricVariableInstance> vars = processEngineConfiguration.getHistoryService().createHistoricVariableInstanceQuery()
                        .processInstanceId(execution.getProcessInstanceId())
                        .variableName("csApproveResult")
                        .list();
                Integer passCount = 0;
                Integer notPassCount = 0;
                for (HistoricVariableInstance value : vars) {
                    if ("pass".equals(value.getValue().toString())) {
                        passCount++;
                    }
                    if ("notpass".equals(value.getValue().toString())) {
                        notPassCount++;
                    }
                }
                LOGGER.info("Countersign result: pass:{} not pass:{}", passCount, notPassCount);
                execution.setVariable("approveResult", passCount > notPassCount ? "pass" : "notpass");
                
            } else {
                execution.setVariable("approveResult", "pass");
            }
        }
    }
}
