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

import java.util.List;

import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngines;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
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
}
