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
package org.flowable.spring.test.executionListener;

import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Yvo Swillens
 */
public class MyTransactionalOperationTransactionDependentExecutionListener extends CurrentActivityTransactionDependentExecutionListener {

    private static final long serialVersionUID = 1L;

    @Override
    public void notify(String processInstanceId, String executionId, FlowElement currentFlowElement,
            Map<String, Object> executionVariables, Map<String, Object> customPropertiesMap) {

        super.notify(processInstanceId, executionId, currentFlowElement, executionVariables, customPropertiesMap);

        if (CommandContextUtil.getProcessEngineConfiguration().getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoryService historyService = CommandContextUtil.getProcessEngineConfiguration().getHistoryService();

            // delete first historic instance
            List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();
            historyService.deleteHistoricProcessInstance(historicProcessInstances.get(0).getId());
        }
    }
}
