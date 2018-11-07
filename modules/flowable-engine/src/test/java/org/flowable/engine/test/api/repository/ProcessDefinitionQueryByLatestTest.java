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
package org.flowable.engine.test.api.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.junit.jupiter.api.Test;

public class ProcessDefinitionQueryByLatestTest extends PluggableFlowableTestCase {

    private static final String XML_FILE_PATH = "org/flowable/engine/test/repository/latest/";

    protected List<String> deploy(List<String> xmlFileNameList) throws Exception {
        List<String> deploymentIdList = new ArrayList<>();
        for (String xmlFileName : xmlFileNameList) {
            String deploymentId = repositoryService
                    .createDeployment()
                    .name(XML_FILE_PATH + xmlFileName)
                    .addClasspathResource(XML_FILE_PATH + xmlFileName)
                    .deploy()
                    .getId();
            deploymentIdList.add(deploymentId);
        }
        return deploymentIdList;
    }

    private void unDeploy(List<String> deploymentIdList) throws Exception {
        for (String deploymentId : deploymentIdList) {
            repositoryService.deleteDeployment(deploymentId, true);
        }
    }

    @Test
    public void testQueryByLatestAndId() throws Exception {
        // Deploy
        List<String> xmlFileNameList = Arrays.asList("name_testProcess1_one.bpmn20.xml",
                "name_testProcess1_two.bpmn20.xml", "name_testProcess2_one.bpmn20.xml");
        List<String> deploymentIdList = deploy(xmlFileNameList);

        List<String> processDefinitionIdList = new ArrayList<>();
        for (String deploymentId : deploymentIdList) {
            String processDefinitionId = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).list().get(0).getId();
            processDefinitionIdList.add(processDefinitionId);
        }

        ProcessDefinitionQuery idQuery1 = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionIdList.get(0)).latestVersion();
        List<ProcessDefinition> processDefinitions = idQuery1.list();
        assertEquals(0, processDefinitions.size());

        ProcessDefinitionQuery idQuery2 = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionIdList.get(1)).latestVersion();
        processDefinitions = idQuery2.list();
        assertEquals(1, processDefinitions.size());
        assertEquals("testProcess1", processDefinitions.get(0).getKey());

        ProcessDefinitionQuery idQuery3 = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionIdList.get(2)).latestVersion();
        processDefinitions = idQuery3.list();
        assertEquals(1, processDefinitions.size());
        assertEquals("testProcess2", processDefinitions.get(0).getKey());

        // Undeploy
        unDeploy(deploymentIdList);
    }

    @Test
    public void testQueryByLatestAndName() throws Exception {
        // Deploy
        List<String> xmlFileNameList = Arrays.asList("name_testProcess1_one.bpmn20.xml",
                "name_testProcess1_two.bpmn20.xml", "name_testProcess2_one.bpmn20.xml");
        List<String> deploymentIdList = deploy(xmlFileNameList);

        // name
        ProcessDefinitionQuery nameQuery = repositoryService.createProcessDefinitionQuery().processDefinitionName("one").latestVersion();
        List<ProcessDefinition> processDefinitions = nameQuery.list();
        assertEquals(1, processDefinitions.size());
        assertEquals(1, processDefinitions.get(0).getVersion());
        assertEquals("testProcess2", processDefinitions.get(0).getKey());

        // nameLike
        ProcessDefinitionQuery nameLikeQuery = repositoryService.createProcessDefinitionQuery().processDefinitionName("one").latestVersion();
        processDefinitions = nameLikeQuery.list();
        assertEquals(1, processDefinitions.size());
        assertEquals(1, processDefinitions.get(0).getVersion());
        assertEquals("testProcess2", processDefinitions.get(0).getKey());

        // Undeploy
        unDeploy(deploymentIdList);
    }

    @Test
    public void testQueryByLatestAndVersion() throws Exception {
        // Deploy
        List<String> xmlFileNameList = Arrays.asList("version_testProcess1_one.bpmn20.xml",
                "version_testProcess1_two.bpmn20.xml", "version_testProcess2_one.bpmn20.xml");
        List<String> deploymentIdList = deploy(xmlFileNameList);

        // version
        ProcessDefinitionQuery nameQuery = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(1).latestVersion();
        List<ProcessDefinition> processDefinitions = nameQuery.list();
        assertEquals(1, processDefinitions.size());
        assertEquals("testProcess2", processDefinitions.get(0).getKey());

        // Undeploy
        unDeploy(deploymentIdList);
    }

    @Test
    public void testQueryByLatestAndDeploymentId() throws Exception {
        // Deploy
        List<String> xmlFileNameList = Arrays.asList("name_testProcess1_one.bpmn20.xml",
                "name_testProcess1_two.bpmn20.xml", "name_testProcess2_one.bpmn20.xml");
        List<String> deploymentIdList = deploy(xmlFileNameList);

        // deploymentId
        ProcessDefinitionQuery deploymentQuery1 = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentIdList.get(0)).latestVersion();
        List<ProcessDefinition> processDefinitions = deploymentQuery1.list();
        assertEquals(0, processDefinitions.size());

        ProcessDefinitionQuery deploymentQuery2 = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentIdList.get(1)).latestVersion();
        processDefinitions = deploymentQuery2.list();
        assertEquals(1, processDefinitions.size());
        assertEquals("testProcess1", processDefinitions.get(0).getKey());

        // Undeploy
        unDeploy(deploymentIdList);
    }
}
