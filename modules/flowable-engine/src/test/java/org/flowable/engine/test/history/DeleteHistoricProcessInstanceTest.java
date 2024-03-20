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
package org.flowable.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * @author Christopher Welsch
 */
public class DeleteHistoricProcessInstanceTest extends PluggableFlowableTestCase {

    protected HistoryLevel engineHistoryLevel;

    @BeforeEach
    public void prepare() {
        engineHistoryLevel = processEngineConfiguration.getHistoryLevel();
    }

    @AfterEach
    public void cleanup() {
        processEngineConfiguration.setHistoryLevel(engineHistoryLevel);
    }

    @ParameterizedTest
    @EnumSource
    @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelNoneProcess.bpmn20.xml" })
    public void testDeleteVariableInstancesWithHistoryLevelNone(HistoryLevel historyLevel) {
        processEngineConfiguration.setHistoryLevel(historyLevel);

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                .variable("testVariable", "testValue").start();

        List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
        assertThat(variableInstances).extracting(VariableInstance::getName, VariableInstance::getValue).contains(
                tuple("testVariable", "testValue")
        );
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId()).list();

        assertThat(historicVariableInstances).isEmpty();
        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult()).isNull();
    }

    @ParameterizedTest
    @EnumSource
    @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelTaskProcess.bpmn20.xml" })
    public void testDeleteVariableInstancesWithHistoryLevelTask(HistoryLevel historyLevel) {
        processEngineConfiguration.setHistoryLevel(historyLevel);
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                .variable("testVariable", "testValue").start();

        List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
        assertThat(variableInstances).extracting(VariableInstance::getName, VariableInstance::getValue).contains(
                tuple("testVariable", "testValue")
        );
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId()).list();

        assertThat(historicVariableInstances).isEmpty();

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(historicProcessInstance).isNotNull();
        assertThat(historicProcessInstance.getId()).isEqualTo(processInstance.getId());

    }

    @ParameterizedTest
    @EnumSource
    @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelInstanceProcess.bpmn20.xml" })
    public void testDeleteVariableInstancesWithHistoryLevelInstance(HistoryLevel historyLevel) {
        processEngineConfiguration.setHistoryLevel(historyLevel);

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                .variable("testVariable", "testValue").start();

        List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
        assertThat(variableInstances).extracting(VariableInstance::getName, VariableInstance::getValue).contains(
                tuple("testVariable", "testValue")
        );
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId()).list();

        assertThat(historicVariableInstances).isEmpty();

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(historicProcessInstance).isNotNull();

    }

    @ParameterizedTest
    @EnumSource
    @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelActivityProcess.bpmn20.xml" })
    public void testDeleteVariableInstancesWithHistoryLevelActivity(HistoryLevel historyLevel) {
        processEngineConfiguration.setHistoryLevel(historyLevel);
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                .variable("testVariable", "testValue").start();

        List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
        assertThat(variableInstances).extracting(VariableInstance::getName, VariableInstance::getValue).contains(
                tuple("testVariable", "testValue")
        );
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId()).list();

        assertThat(historicVariableInstances).extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue).contains(
                tuple("testVariable", "testValue")
        );
    }

    @ParameterizedTest
    @EnumSource
    @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelAuditProcess.bpmn20.xml" })
    public void testDeleteVariableInstancesWithHistoryLevelAudit(HistoryLevel historyLevel) {
        processEngineConfiguration.setHistoryLevel(historyLevel);
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                .variable("testVariable", "testValue").start();

        List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
        assertThat(variableInstances).extracting(VariableInstance::getName, VariableInstance::getValue).contains(
                tuple("testVariable", "testValue")
        );
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId()).list();

        assertThat(historicVariableInstances).extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue).contains(
                tuple("testVariable", "testValue")
        );
    }

    @ParameterizedTest
    @EnumSource
    @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelFullProcess.bpmn20.xml" })
    public void testDeleteVariableInstancesWithHistoryLevelFull(HistoryLevel historyLevel) {
        processEngineConfiguration.setHistoryLevel(historyLevel);
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                .variable("testVariable", "testValue").start();

        List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
        assertThat(variableInstances).extracting(VariableInstance::getName, VariableInstance::getValue).contains(
                tuple("testVariable", "testValue")
        );
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId()).list();

        assertThat(historicVariableInstances).extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue).contains(
                tuple("testVariable", "testValue")
        );
    }
}
