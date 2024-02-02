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
package org.flowable.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class ProcessInstanceUpdateBusinessKeyTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testProcessInstanceUpdateBusinessKey() {
        runtimeService.startProcessInstanceByKey("businessKeyProcess");

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance.getBusinessKey()).isEqualTo("bzKey");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
            assertThat(historicProcessInstance.getBusinessKey()).isEqualTo("bzKey");
        }
    }

    @Test
    @Deployment
    public void testUpdateExistingBusinessKey() {
        runtimeService.startProcessInstanceByKey("businessKeyProcess", "testKey");

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance.getBusinessKey()).isEqualTo("testKey");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
            assertThat(historicProcessInstance.getBusinessKey()).isEqualTo("testKey");
        }

        runtimeService.updateBusinessKey(processInstance.getId(), "newKey");

        processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance.getBusinessKey()).isEqualTo("newKey");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
            assertThat(historicProcessInstance.getBusinessKey()).isEqualTo("newKey");
        }
    }

    public static class UpdateBusinessKeyExecutionListener implements ExecutionListener {

        private static final long serialVersionUID = 1L;

        @Override
        public void notify(DelegateExecution delegateExecution) {
            CommandContextUtil.getExecutionEntityManager().updateProcessInstanceBusinessKey((ExecutionEntity) delegateExecution, "bzKey");
        }
    }

}
