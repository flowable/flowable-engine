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
import static org.assertj.core.api.Assertions.entry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class ProcessInstanceAndVariablesQueryTest extends PluggableFlowableTestCase {

    private static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";
    private static final String PROCESS_DEFINITION_KEY_2 = "oneTaskProcess2";
    private static final String PROCESS_DEFINITION_KEY_3 = "oneTaskProcess3";
    private static final String PROCESS_DEFINITION_KEY_4 = "oneTaskProcess4";

    /**
     * Setup starts 4 process instances of oneTaskProcess and 1 instance of oneTaskProcess2
     * oneTaskProcess4 contains a task variable added via an execution task listener (which shouldn't be returned in the query)
     */
    @BeforeEach
    protected void setUp() throws Exception {
        repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess2.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess3.bpmn20.xml")
            .addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess4.bpmn20.xml")
                .deploy();

        Map<String, Object> startMap = new HashMap<>();
        startMap.put("test", "test");
        startMap.put("test2", "test2");
        for (int i = 0; i < 4; i++) {
            runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, String.valueOf(i), startMap);
        }

        startMap.clear();
        startMap.put("anothertest", 123);
        runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY_2, "1", startMap);

        startMap.clear();
        startMap.put("casetest", "MyCaseTest");
        runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY_3, "1", startMap);

        startMap.clear();
        startMap.put("test4", "test4");
        runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY_4, "1", startMap);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    public void testQuery() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().includeProcessVariables().variableValueEquals("anothertest", 123).singleResult();
        Map<String, Object> variableMap = processInstance.getProcessVariables();
        assertThat(variableMap)
                .containsOnly(entry("anothertest", 123));

        List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery().includeProcessVariables().list();
        assertThat(instanceList).hasSize(7);

        processInstance = runtimeService.createProcessInstanceQuery().includeProcessVariables()
                .variableValueLike("casetest", "MyCase%").singleResult();
        variableMap = processInstance.getProcessVariables();
        assertThat(variableMap)
                .containsOnly(entry("casetest", "MyCaseTest"));

        processInstance = runtimeService.createProcessInstanceQuery().includeProcessVariables()
                .variableValueLikeIgnoreCase("casetest", "mycase%").singleResult();
        variableMap = processInstance.getProcessVariables();
        assertThat(variableMap)
                .containsOnly(entry("casetest", "MyCaseTest"));

        processInstance = runtimeService.createProcessInstanceQuery().includeProcessVariables()
                .variableValueLikeIgnoreCase("casetest", "mycase2%").singleResult();
        assertThat(processInstance).isNull();

        instanceList = runtimeService.createProcessInstanceQuery().includeProcessVariables().processDefinitionKey(PROCESS_DEFINITION_KEY).list();
        assertThat(instanceList).hasSize(4);
        processInstance = instanceList.get(0);
        variableMap = processInstance.getProcessVariables();
        assertThat(variableMap)
                .containsOnly(
                        entry("test", "test"),
                        entry("test2", "test2")
                );

        processInstance = runtimeService.createProcessInstanceQuery().includeProcessVariables().processDefinitionKey(PROCESS_DEFINITION_KEY_2).singleResult();
        variableMap = processInstance.getProcessVariables();
        assertThat(variableMap)
                .containsOnly(entry("anothertest", 123));

        instanceList = runtimeService.createProcessInstanceQuery().includeProcessVariables().processDefinitionKey(PROCESS_DEFINITION_KEY).listPage(0, 5);
        assertThat(instanceList).hasSize(4);
        processInstance = instanceList.get(0);
        variableMap = processInstance.getProcessVariables();
        assertThat(variableMap)
                .containsOnly(
                        entry("test", "test"),
                        entry("test2", "test2")
                );

        instanceList = runtimeService.createProcessInstanceQuery().includeProcessVariables().processDefinitionKey(PROCESS_DEFINITION_KEY).listPage(0, 1);
        assertThat(instanceList).hasSize(1);
        processInstance = instanceList.get(0);
        variableMap = processInstance.getProcessVariables();
        assertThat(variableMap)
                .containsOnly(
                        entry("test", "test"),
                        entry("test2", "test2")
                );

        instanceList = runtimeService.createProcessInstanceQuery().includeProcessVariables().processDefinitionKey(PROCESS_DEFINITION_KEY).orderByProcessDefinitionKey().asc().listPage(2, 4);
        assertThat(instanceList).hasSize(2);
        processInstance = instanceList.get(0);
        variableMap = processInstance.getProcessVariables();
        assertThat(variableMap)
                .containsOnly(
                        entry("test", "test"),
                        entry("test2", "test2")
                );

        instanceList = runtimeService.createProcessInstanceQuery().includeProcessVariables().processDefinitionKey(PROCESS_DEFINITION_KEY).orderByProcessDefinitionKey().asc().listPage(4, 5);
        assertThat(instanceList).isEmpty();

        processInstance = runtimeService.createProcessInstanceQuery().includeProcessVariables().variableValueEquals("test4", "test4").singleResult();
        variableMap = processInstance.getProcessVariables();
        assertThat(variableMap)
                .containsOnly(entry("test4", "test4"));
    }

    @Test
    public void testOrQuery() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().includeProcessVariables()
                .or().variableValueEquals("undefined", 999).variableValueEquals("anothertest", 123).endOr().singleResult();
        Map<String, Object> variableMap = processInstance.getProcessVariables();
        assertThat(variableMap)
                .containsOnly(entry("anothertest", 123));

        processInstance = runtimeService.createProcessInstanceQuery().includeProcessVariables()
                .or().variableValueEquals("undefined", 999).endOr().singleResult();
        assertThat(processInstance).isNull();

        processInstance = runtimeService.createProcessInstanceQuery().includeProcessVariables()
                .or().variableValueEquals("anothertest", 123).variableValueEquals("undefined", 999).endOr().singleResult();
        variableMap = processInstance.getProcessVariables();
        assertThat(variableMap)
                .containsOnly(entry("anothertest", 123));

        processInstance = runtimeService.createProcessInstanceQuery().includeProcessVariables()
                .or().variableValueEquals("anothertest", 999).endOr().singleResult();
        assertThat(processInstance).isNull();

        processInstance = runtimeService.createProcessInstanceQuery().includeProcessVariables()
                .or().variableValueEquals("anothertest", 999).variableValueEquals("anothertest", 123).endOr().singleResult();
        variableMap = processInstance.getProcessVariables();
        assertThat(variableMap)
                .containsOnly(entry("anothertest", 123));
    }

    @Test
    public void testOrQueryMultipleVariableValues() {
        ProcessInstanceQuery query0 = runtimeService.createProcessInstanceQuery().includeProcessVariables().or();
        for (int i = 0; i < 20; i++) {
            query0 = query0.variableValueEquals("anothertest", i);
        }
        query0 = query0.endOr();
        assertThat(query0.singleResult()).isNull();

        ProcessInstanceQuery query1 = runtimeService.createProcessInstanceQuery().includeProcessVariables().or().variableValueEquals("anothertest", 123);
        for (int i = 0; i < 20; i++) {
            query1 = query1.variableValueEquals("anothertest", i);
        }
        query1 = query1.endOr();
        assertThat(query0.singleResult()).isNull();

        ProcessInstance processInstance = query1.singleResult();
        Map<String, Object> variableMap = processInstance.getProcessVariables();
        assertThat(variableMap)
                .containsOnly(entry("anothertest", 123));

        ProcessInstanceQuery query2 = runtimeService.createProcessInstanceQuery().includeProcessVariables().or();
        for (int i = 0; i < 20; i++) {
            query2 = query2.variableValueEquals("anothertest", i);
        }
        query2 = query2.endOr()
                .or()
                .processDefinitionKey(PROCESS_DEFINITION_KEY_2)
                .processDefinitionId("undefined")
                .endOr();
        assertThat(query2.singleResult()).isNull();

        ProcessInstanceQuery query3 = runtimeService.createProcessInstanceQuery().includeProcessVariables().or().variableValueEquals("anothertest", 123);
        for (int i = 0; i < 20; i++) {
            query3 = query3.variableValueEquals("anothertest", i);
        }
        query3 = query3.endOr()
                .or()
                .processDefinitionKey(PROCESS_DEFINITION_KEY_2)
                .processDefinitionId("undefined")
                .endOr();
        variableMap = query3.singleResult().getProcessVariables();
        assertThat(variableMap)
                .containsOnly(entry("anothertest", 123));
    }

    @Test
    public void testOrProcessVariablesLikeIgnoreCase() {
        List<ProcessInstance> instanceList = runtimeService
                .createProcessInstanceQuery().or()
                .variableValueLikeIgnoreCase("test", "TES%")
                .variableValueLikeIgnoreCase("test", "%XYZ").endOr()
                .list();
        assertThat(instanceList).hasSize(4);
    }

}
