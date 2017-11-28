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
package org.flowable.dmn.engine.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;

import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngines;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;

/**
 * @author Yvo Swillens
 */
public class MixedDeploymentTest extends AbstractFlowableDmnEngineConfiguratorTest {

    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn" })
    public void deploySingleProcessAndDecisionTable() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .processDefinitionKey("oneDecisionTaskProcess")
                .singleResult();

        assertNotNull(processDefinition);
        assertEquals("oneDecisionTaskProcess", processDefinition.getKey());

        DmnRepositoryService dmnRepositoryService = DmnEngines.getDefaultDmnEngine().getDmnRepositoryService();
        DmnDecisionTable decisionTable = dmnRepositoryService.createDecisionTableQuery()
                .latestVersion()
                .decisionTableKey("decision1")
                .singleResult();
        assertNotNull(decisionTable);
        assertEquals("decision1", decisionTable.getKey());

        List<DmnDecisionTable> decisionTableList = repositoryService.getDecisionTablesForProcessDefinition(processDefinition.getId());
        assertEquals(1l, decisionTableList.size());
        assertEquals("decision1", decisionTableList.get(0).getKey());
    }
    
    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn" })
    public void testDecisionTaskExecution() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneDecisionTaskProcess", Collections.singletonMap("inputVariable1", (Object) 1));
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstance.getId()).orderByVariableName().asc().list();
        
        assertEquals("inputVariable1", variables.get(0).getVariableName());
        assertEquals(1, variables.get(0).getValue());
        assertEquals("outputVariable1", variables.get(1).getVariableName());
        assertEquals("result1", variables.get(1).getValue());
    }
    
    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcess.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn" })
    public void testFailedDecisionTask() {
        try {
            runtimeService.startProcessInstanceByKey("oneDecisionTaskProcess");
            fail("Expected DMN failure due to missing variable");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unknown property used in expression: #{inputVariable1"));
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/dmn/engine/test/deployment/oneDecisionTaskNoHitsErrorProcess.bpmn20.xml",
            "org/flowable/dmn/engine/test/deployment/simple.dmn" })
    public void testNoHitsDecisionTask() {
        try {
            runtimeService.startProcessInstanceByKey("oneDecisionTaskProcess", Collections.singletonMap("inputVariable1", (Object) 2));
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("did not hit any rules for the provided input"));
        }
    }
}
