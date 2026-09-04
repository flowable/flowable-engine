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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class ProcessInstanceUpdateBuilderTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testUpdateBusinessKey() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.createProcessInstanceUpdateBuilder(processInstance.getId())
                .businessKey("newBusinessKey")
                .update();

        ProcessInstance updatedInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId()).singleResult();
        assertThat(updatedInstance.getBusinessKey()).isEqualTo("newBusinessKey");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId()).singleResult();
            assertThat(historicProcessInstance.getBusinessKey()).isEqualTo("newBusinessKey");
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testUpdateBusinessStatus() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.createProcessInstanceUpdateBuilder(processInstance.getId())
                .businessStatus("newStatus")
                .update();

        ProcessInstance updatedInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId()).singleResult();
        assertThat(updatedInstance.getBusinessStatus()).isEqualTo("newStatus");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId()).singleResult();
            assertThat(historicProcessInstance.getBusinessStatus()).isEqualTo("newStatus");
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testUpdateName() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance.getName()).isNull();

        runtimeService.createProcessInstanceUpdateBuilder(processInstance.getId())
                .name("My process")
                .update();

        ProcessInstance updatedInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId()).singleResult();
        assertThat(updatedInstance.getName()).isEqualTo("My process");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testUpdateMultipleProperties() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.createProcessInstanceUpdateBuilder(processInstance.getId())
                .businessKey("myKey")
                .businessStatus("myStatus")
                .name("My process")
                .update();

        ProcessInstance updatedInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId()).singleResult();
        assertThat(updatedInstance.getBusinessKey()).isEqualTo("myKey");
        assertThat(updatedInstance.getBusinessStatus()).isEqualTo("myStatus");
        assertThat(updatedInstance.getName()).isEqualTo("My process");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId()).singleResult();
            assertThat(historicProcessInstance.getBusinessKey()).isEqualTo("myKey");
            assertThat(historicProcessInstance.getBusinessStatus()).isEqualTo("myStatus");
            assertThat(historicProcessInstance.getName()).isEqualTo("My process");
        }
    }

    @Test
    public void testUpdateWithNullId() {
        assertThatThrownBy(() -> runtimeService.createProcessInstanceUpdateBuilder(null)
                .businessKey("key")
                .update())
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testUpdateWithNonExistingId() {
        assertThatThrownBy(() -> runtimeService.createProcessInstanceUpdateBuilder("nonExistingId")
                .businessKey("key")
                .update())
                .isInstanceOf(FlowableObjectNotFoundException.class);
    }
}
