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
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

class ProcessInstanceCreateWithBusinessStatusTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    void testProcessInstanceCreateWithBusinessStatus() {

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                .businessStatus("testStatus").start();
        assertThat(processInstance.getBusinessStatus()).isEqualTo("testStatus");

        processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance.getBusinessStatus()).isEqualTo("testStatus");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
            assertThat(historicProcessInstance.getBusinessStatus()).isEqualTo("testStatus");
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    void testProcessInstanceCreateWithoutBusinessStatus() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").start();
        assertThat(processInstance.getBusinessStatus()).isNull();

        processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance.getBusinessStatus()).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
            assertThat(historicProcessInstance.getBusinessStatus()).isNull();
        }
    }

}
