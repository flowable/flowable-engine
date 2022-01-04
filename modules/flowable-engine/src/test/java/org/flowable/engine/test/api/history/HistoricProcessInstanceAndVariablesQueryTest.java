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
package org.flowable.engine.test.api.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class HistoricProcessInstanceAndVariablesQueryTest extends PluggableFlowableTestCase {

    private static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";
    private static final String PROCESS_DEFINITION_KEY_2 = "oneTaskProcess2";
    private static final String PROCESS_DEFINITION_NAME_2 = "oneTaskProcess2Name";
    private static final String PROCESS_DEFINITION_CATEGORY_2 = "org.flowable.engine.test.api.runtime.2Category";
    private static final String PROCESS_DEFINITION_KEY_3 = "oneTaskProcess3";

    private List<String> processInstanceIds;

    /**
     * Setup starts 4 process instances of oneTaskProcess and 1 instance of oneTaskProcess2
     */
    @BeforeEach
    protected void setUp() throws Exception {
        repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess2.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess3.bpmn20.xml")
                .deploy();

        Map<String, Object> startMap = new HashMap<>();
        startMap.put("test", "test");
        startMap.put("test2", "test2");
        processInstanceIds = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            processInstanceIds.add(runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, String.valueOf(i), startMap).getId());
            if (i == 0) {
                org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstanceIds.get(0)).singleResult();
                taskService.complete(task.getId());
            }
        }
        startMap.clear();
        startMap.put("anothertest", 123);
        processInstanceIds.add(runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY_2, "1", startMap).getId());

        startMap.clear();
        startMap.put("casetest", "MyTest");
        processInstanceIds.add(runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY_3, "1", startMap).getId());
    }

    @AfterEach
    protected void tearDown() throws Exception {
        deleteDeployments();
    }

    @Test
    public void testQuery() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("anothertest", 123).singleResult();
            Map<String, Object> variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsExactly(entry("anothertest", 123));

            List<HistoricProcessInstance> instanceList = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().list();
            assertThat(instanceList).hasSize(6);

            instanceList = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().processDefinitionKey(PROCESS_DEFINITION_KEY).list();
            assertThat(instanceList).hasSize(4);
            processInstance = instanceList.get(0);
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("test", "test"),
                            entry("test2", "test2"));
            assertThat(instanceList.get(0).getProcessDefinitionKey()).isEqualTo(PROCESS_DEFINITION_KEY);

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().processDefinitionKey(PROCESS_DEFINITION_KEY_2)
                    .singleResult();
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsExactly(entry("anothertest", 123));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().finished().singleResult();
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("test", "test"),
                            entry("test2", "test2"));

            instanceList = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().listPage(0, 50);
            assertThat(instanceList).hasSize(6);
            assertThat(historyService.createHistoricProcessInstanceQuery().includeProcessVariables().count()).isEqualTo(6);

            instanceList = historyService.createHistoricProcessInstanceQuery()
                    .variableValueEquals("test", "test")
                    .includeProcessVariables()
                    .listPage(0, 50);
            assertThat(instanceList).hasSize(4);
            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("test", "test").includeProcessVariables().count()).isEqualTo(4);

            instanceList = historyService.createHistoricProcessInstanceQuery()
                    .variableValueLike("test", "te%")
                    .includeProcessVariables()
                    .list();
            assertThat(instanceList).hasSize(4);
            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLike("test", "te%").includeProcessVariables().count()).isEqualTo(4);

            instanceList = historyService.createHistoricProcessInstanceQuery()
                    .variableValueLike("test2", "te%2")
                    .includeProcessVariables()
                    .list();
            assertThat(instanceList).hasSize(4);
            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLike("test2", "te%2").includeProcessVariables().count()).isEqualTo(4);

            instanceList = historyService.createHistoricProcessInstanceQuery()
                    .variableValueLikeIgnoreCase("test", "te%")
                    .includeProcessVariables()
                    .list();
            assertThat(instanceList).hasSize(4);
            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLikeIgnoreCase("test", "te%").includeProcessVariables().count())
                    .isEqualTo(4);

            instanceList = historyService.createHistoricProcessInstanceQuery()
                    .variableValueLikeIgnoreCase("test", "t3%")
                    .includeProcessVariables()
                    .list();
            assertThat(instanceList).isEmpty();
            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLikeIgnoreCase("test", "t3%").includeProcessVariables().count())
                    .isZero();

            instanceList = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().listPage(0, 50);
            assertThat(instanceList).hasSize(6);
            assertThat(historyService.createHistoricProcessInstanceQuery().includeProcessVariables().count()).isEqualTo(6);

            instanceList = historyService.createHistoricProcessInstanceQuery()
                    .variableValueEquals("test", "test")
                    .includeProcessVariables()
                    .listPage(0, 1);
            assertThat(instanceList).hasSize(1);
            processInstance = instanceList.get(0);
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("test", "test"),
                            entry("test2", "test2"));
            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("test", "test").includeProcessVariables().count()).isEqualTo(4);

            instanceList = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().processDefinitionKey(PROCESS_DEFINITION_KEY)
                    .listPage(1, 2);
            assertThat(instanceList).hasSize(2);
            processInstance = instanceList.get(0);
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("test", "test"),
                            entry("test2", "test2"));

            instanceList = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().processDefinitionKey(PROCESS_DEFINITION_KEY)
                    .listPage(3, 4);
            assertThat(instanceList).hasSize(1);
            processInstance = instanceList.get(0);
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("test", "test"),
                            entry("test2", "test2"));

            instanceList = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().processDefinitionKey(PROCESS_DEFINITION_KEY)
                    .listPage(4, 2);
            assertThat(instanceList).isEmpty();

            instanceList = historyService.createHistoricProcessInstanceQuery().variableValueEquals("test", "test").includeProcessVariables()
                    .orderByProcessInstanceId().asc().listPage(0, 50);
            assertThat(instanceList).hasSize(4);
        }
    }
    
    @Test
    public void testQueryOnTaskVariable() {
        ProcessInstance taskProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess3").singleResult();
        Task task = taskService.createTaskQuery().processInstanceId(taskProcessInstance.getId()).singleResult();
        taskService.setVariableLocal(task.getId(), "localVar", "test");
        
        assertThat(runtimeService.createProcessInstanceQuery().variableValueEquals("localVar", "test").list()).isEmpty();
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("anothertest", 123).singleResult();
            Map<String, Object> variableMap = processInstance.getProcessVariables();
            assertThat(variableMap).containsExactly(entry("anothertest", 123));
            
            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("localVar", "test").list()).isEmpty();
            assertThat(historyService.createHistoricProcessInstanceQuery().localVariableValueEquals("localVar", "test").list()).hasSize(1);
        }
    }

    @Test
    public void testQueryByProcessDefinition() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // DeploymentId
            String deploymentId = repositoryService.createDeploymentQuery().list().get(0).getId();
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("anothertest", 123).deploymentId(deploymentId).singleResult();
            Map<String, Object> variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsExactly(entry("anothertest", 123));
            assertThat(processInstance.getDeploymentId()).isEqualTo(deploymentId);

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("anothertest", "invalid").deploymentId(deploymentId).singleResult();
            assertThat(processInstance).isNull();

            // ProcessDefinitionName
            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("anothertest", 123).processDefinitionName(PROCESS_DEFINITION_NAME_2).singleResult();
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsExactly(entry("anothertest", 123));
            assertThat(processInstance.getProcessDefinitionName()).isEqualTo(PROCESS_DEFINITION_NAME_2);

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("test", "test").processDefinitionName(PROCESS_DEFINITION_NAME_2).singleResult();
            assertThat(processInstance).isNull();

            // ProcessDefinitionCategory
            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("anothertest", 123).processDefinitionCategory(PROCESS_DEFINITION_CATEGORY_2).singleResult();
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsExactly(entry("anothertest", 123));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("test", "test").processDefinitionCategory(PROCESS_DEFINITION_CATEGORY_2).singleResult();
            assertThat(processInstance).isNull();
        }
    }

    @Test
    public void testQueryByVariableExist() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // DeploymentId
            String deploymentId = repositoryService.createDeploymentQuery().list().get(0).getId();
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().variableExists("anothertest")
                    .deploymentId(deploymentId).singleResult();
            assertThat(processInstance).isNotNull();

            List<HistoricProcessInstance> processInstanceList = historyService.createHistoricProcessInstanceQuery()
                    .variableNotExists("anothertest").deploymentId(deploymentId).list();
            assertThat(processInstanceList).hasSize(5);

            processInstance = historyService.createHistoricProcessInstanceQuery().or().variableExists("anothertest")
                    .processInstanceId("notexisting").endOr().deploymentId(deploymentId).singleResult();
            assertThat(processInstance).isNotNull();

            processInstanceList = historyService.createHistoricProcessInstanceQuery().or().variableNotExists("anothertest")
                    .processInstanceId("nonexisting").endOr().deploymentId(deploymentId).list();
            assertThat(processInstanceList).hasSize(5);

            processInstanceList = historyService.createHistoricProcessInstanceQuery().or().variableNotExists("anothertest")
                    .variableValueEquals("test", "test").endOr().deploymentId(deploymentId).list();
            assertThat(processInstanceList).hasSize(5);

            processInstanceList = historyService.createHistoricProcessInstanceQuery().or().variableNotExists("anothertest").endOr().or()
                    .variableValueEquals("test", "test").endOr().deploymentId(deploymentId).list();
            assertThat(processInstanceList).hasSize(4);
        }
    }

    @Test
    public void testOrQuery() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().or()
                    .variableValueEquals("anothertest", 123)
                    .processDefinitionId("undefined").endOr().singleResult();
            Map<String, Object> variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsExactly(entry("anothertest", 123));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or()
                    .variableValueEquals("anothertest", 123)
                    .processDefinitionId("undefined")
                    .endOr()
                    .or()
                    .processDefinitionKey(PROCESS_DEFINITION_KEY_2)
                    .processDefinitionId("undefined")
                    .endOr()
                    .singleResult();
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsExactly(entry("anothertest", 123));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or()
                    .variableValueEquals("anothertest", 123)
                    .processDefinitionId("undefined")
                    .endOr()
                    .or()
                    .processDefinitionKey(PROCESS_DEFINITION_KEY)
                    .processDefinitionId("undefined")
                    .endOr()
                    .singleResult();
            assertThat(processInstance).isNull();

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or()
                    .variableValueLikeIgnoreCase("casetest", "mytest")
                    .processDefinitionId("undefined")
                    .endOr()
                    .singleResult();
            assertThat(processInstance).isNotNull();
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsExactly(entry("casetest", "MyTest"));

            List<HistoricProcessInstance> instanceList = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().or()
                    .processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionId("undefined").endOr().list();
            assertThat(instanceList).hasSize(4);
            processInstance = instanceList.get(0);
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("test", "test"),
                            entry("test2", "test2"));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().or().processDefinitionKey(PROCESS_DEFINITION_KEY_2)
                    .processDefinitionId("undefined").endOr()
                    .singleResult();
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsExactly(entry("anothertest", 123));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().or().finished().processDefinitionId("undefined")
                    .endOr().singleResult();
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("test", "test"),
                            entry("test2", "test2"));

            instanceList = historyService.createHistoricProcessInstanceQuery().or().variableValueEquals("test", "test").processDefinitionId("undefined").endOr()
                    .includeProcessVariables().listPage(0, 50);
            assertThat(instanceList).hasSize(4);

            instanceList = historyService.createHistoricProcessInstanceQuery().or().variableValueEquals("test", "test").processDefinitionId("undefined").endOr()
                    .includeProcessVariables().listPage(0, 1);
            assertThat(instanceList).hasSize(1);
            processInstance = instanceList.get(0);
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("test", "test"),
                            entry("test2", "test2"));

            instanceList = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().or().processDefinitionKey(PROCESS_DEFINITION_KEY)
                    .processDefinitionId("undefined").endOr()
                    .listPage(1, 2);
            assertThat(instanceList).hasSize(2);
            processInstance = instanceList.get(0);
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("test", "test"),
                            entry("test2", "test2"));

            instanceList = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().or().processDefinitionKey(PROCESS_DEFINITION_KEY)
                    .processDefinitionId("undefined").endOr()
                    .listPage(3, 4);
            assertThat(instanceList).hasSize(1);
            processInstance = instanceList.get(0);
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("test", "test"),
                            entry("test2", "test2"));

            instanceList = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().or().processDefinitionKey(PROCESS_DEFINITION_KEY)
                    .processDefinitionId("undefined").endOr()
                    .listPage(4, 2);
            assertThat(instanceList).isEmpty();

            instanceList = historyService.createHistoricProcessInstanceQuery().or().variableValueEquals("test", "test").processDefinitionId("undefined").endOr()
                    .includeProcessVariables()
                    .orderByProcessInstanceId().asc().listPage(0, 50);
            assertThat(instanceList).hasSize(4);

            instanceList = historyService.createHistoricProcessInstanceQuery()
                    .or()
                    .variableValueEquals("test", "test")
                    .processDefinitionId("undefined")
                    .endOr()
                    .or()
                    .processDefinitionKey(PROCESS_DEFINITION_KEY)
                    .processDefinitionId("undefined")
                    .endOr()
                    .includeProcessVariables()
                    .orderByProcessInstanceId()
                    .asc()
                    .listPage(0, 50);
            assertThat(instanceList).hasSize(4);
        }
    }

    @Test
    public void testOrQueryMultipleVariableValues() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstanceQuery query0 = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().or();
            for (int i = 0; i < 20; i++) {
                query0 = query0.variableValueEquals("anothertest", i);
            }
            query0 = query0.processDefinitionId("undefined").endOr();

            assertThat(query0.singleResult()).isNull();

            HistoricProcessInstanceQuery query1 = historyService.createHistoricProcessInstanceQuery().includeProcessVariables().or()
                    .variableValueEquals("anothertest", 123);
            for (int i = 0; i < 20; i++) {
                query1 = query1.variableValueEquals("anothertest", i);
            }
            query1 = query1.processDefinitionId("undefined").endOr();

            HistoricProcessInstance processInstance = query1.singleResult();
            Map<String, Object> variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsExactly(entry("anothertest", 123));

            HistoricProcessInstanceQuery query2 = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or();
            for (int i = 0; i < 20; i++) {
                query2 = query2.variableValueEquals("anothertest", i);
            }
            query2 = query2.processDefinitionId("undefined")
                    .endOr()
                    .or()
                    .processDefinitionKey(PROCESS_DEFINITION_KEY_2)
                    .processDefinitionId("undefined")
                    .endOr();
            assertThat(query2.singleResult()).isNull();

            HistoricProcessInstanceQuery query3 = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or().variableValueEquals("anothertest", 123);
            for (int i = 0; i < 20; i++) {
                query3 = query3.variableValueEquals("anothertest", i);
            }
            query3 = query3.processDefinitionId("undefined")
                    .endOr()
                    .or()
                    .processDefinitionKey(PROCESS_DEFINITION_KEY_2)
                    .processDefinitionId("undefined")
                    .endOr();
            variableMap = query3.singleResult().getProcessVariables();
            assertThat(variableMap)
                    .containsExactly(entry("anothertest", 123));
        }
    }

    @Test
    public void testOrQueryByProcessDefinition() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // DeploymentId
            String deploymentId = repositoryService.createDeploymentQuery().list().get(0).getId();
            HistoricProcessInstanceQuery historicprocessInstanceQuery = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or().variableValueEquals("anothertest", "invalid").deploymentId(deploymentId).endOr();
            assertThat(historicprocessInstanceQuery.list()).hasSize(6);
            assertThat(historicprocessInstanceQuery.count()).isEqualTo(6);
            Map<String, Object> variableMap = historicprocessInstanceQuery.list()
                    .stream()
                    .filter(p -> p.getId().equals(processInstanceIds.get(4)))
                    .map(HistoricProcessInstance::getProcessVariables)
                    .findAny()
                    .orElse(Collections.emptyMap());
            assertThat(variableMap)
                    .containsExactly(entry("anothertest", 123));
            for (HistoricProcessInstance processInstance : historicprocessInstanceQuery.list()) {
                assertThat(processInstance.getDeploymentId()).isEqualTo(deploymentId);
            }

            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or().variableValueEquals("anothertest", "invalid").deploymentId("invalid").endOr().singleResult();
            assertThat(processInstance).isNull();

            // ProcessDefinitionName
            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or().variableValueEquals("anothertest", "invalid").processDefinitionName(PROCESS_DEFINITION_NAME_2).endOr().singleResult();
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsExactly(entry("anothertest", 123));
            assertThat(processInstance.getProcessDefinitionName()).isEqualTo(PROCESS_DEFINITION_NAME_2);

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or().variableValueEquals("anothertest", "invalid").processDefinitionName("invalid").endOr().singleResult();
            assertThat(processInstance).isNull();

            // ProcessDefinitionCategory
            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or().variableValueEquals("anothertest", "invalid").processDefinitionCategory(PROCESS_DEFINITION_CATEGORY_2).endOr().singleResult();
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsExactly(entry("anothertest", 123));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or().variableValueEquals("anothertest", "invalid").processDefinitionCategory("invalid").endOr().singleResult();
            assertThat(processInstance).isNull();
        }
    }
}
