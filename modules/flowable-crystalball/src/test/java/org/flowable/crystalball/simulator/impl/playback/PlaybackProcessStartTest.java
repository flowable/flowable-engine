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
package org.flowable.crystalball.simulator.impl.playback;

import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.crystalball.simulator.impl.EventRecorderTestUtils;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;

/**
 * @author martin.grofcik
 */
public class PlaybackProcessStartTest extends AbstractPlaybackTest {
    public static final String SIMPLEST_PROCESS = "theSimplestProcess";
    public static final String BUSINESS_KEY = "testBusinessKey";
    public static final String TEST_VALUE = "TestValue";
    public static final String TEST_VARIABLE = "testVariable";

    @CheckStatus(methodName = "demoCheckStatus")
    @Deployment
    public void testDemo() {
        Map<String, Object> variables = new HashMap<>();
        variables.put(TEST_VARIABLE, TEST_VALUE);
        processEngine.getRuntimeService().startProcessInstanceByKey(SIMPLEST_PROCESS, BUSINESS_KEY, variables);
    }

    public void demoCheckStatus() {
        final HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().finished().includeProcessVariables()
                .singleResult();
        assertThat(historicProcessInstance).isNotNull();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        final ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(historicProcessInstance.getProcessDefinitionId()).singleResult();
        assertThat(processDefinition.getKey()).isEqualTo(SIMPLEST_PROCESS);

        assertThat(historicProcessInstance.getProcessVariables())
                .hasSize(1)
                .containsEntry(TEST_VARIABLE, TEST_VALUE);
        assertThat(historicProcessInstance.getBusinessKey()).isEqualTo(BUSINESS_KEY);
    }

    @CheckStatus(methodName = "messageProcessStartCheckStatus")
    @Deployment
    public void testMessageProcessStart() {

        runtimeService.startProcessInstanceByMessage("startProcessMessage");
    }

    public void messageProcessStartCheckStatus() {
        final HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().finished().singleResult();
        assertThat(historicProcessInstance).isNotNull();
        assertThat(startsWith(historicProcessInstance.getProcessDefinitionId(), "messageStartEventProcess")).isTrue();
    }

    @Deployment
    @CheckStatus(methodName = "checkStatus")
    public void testSignals() throws Exception {
        runtimeService.startProcessInstanceByKey("catchSignal");
        EventRecorderTestUtils.increaseTime(this.processEngineConfiguration.getClock());
        runtimeService.startProcessInstanceByKey("throwSignal");
    }

    public void checkStatus() {
        final List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().finished().list();
        assertThat(historicProcessInstances).hasSize(2);
    }

    @Deployment
    @CheckStatus(methodName = "userTaskCheckStatus")
    public void testUserTask() throws Exception {
        runtimeService.startProcessInstanceByKey("oneTaskProcess", "oneTaskProcessBusinessKey");
        Task task = taskService.createTaskQuery().taskDefinitionKey("userTask").singleResult();
        EventRecorderTestUtils.increaseTime(processEngineConfiguration.getClock());
        taskService.complete(task.getId());
    }

    public void userTaskCheckStatus() {
        final HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().finished().singleResult();
        assertThat(historicProcessInstance).isNotNull();
        assertThat(historicProcessInstance.getBusinessKey()).isEqualTo("oneTaskProcessBusinessKey");
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskDefinitionKey("userTask").singleResult();
        assertThat(historicTaskInstance.getAssignee()).isEqualTo("user1");
    }
}
