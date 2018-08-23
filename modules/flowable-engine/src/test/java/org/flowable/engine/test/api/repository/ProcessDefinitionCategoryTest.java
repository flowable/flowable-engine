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

import java.util.HashSet;
import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class ProcessDefinitionCategoryTest extends PluggableFlowableTestCase {

    @Test
    public void testQueryByCategoryNotEquals() {
        Deployment deployment = repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/api/repository/processCategoryOne.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/api/repository/processCategoryTwo.bpmn20.xml").addClasspathResource("org/flowable/engine/test/api/repository/processCategoryThree.bpmn20.xml")
                .deploy();

        HashSet<String> processDefinitionNames = getProcessDefinitionNames(repositoryService.createProcessDefinitionQuery().processDefinitionCategoryNotEquals("one").list());
        HashSet<String> expectedProcessDefinitionNames = new HashSet<>();
        expectedProcessDefinitionNames.add("processTwo");
        expectedProcessDefinitionNames.add("processThree");
        assertEquals(expectedProcessDefinitionNames, processDefinitionNames);

        processDefinitionNames = getProcessDefinitionNames(repositoryService.createProcessDefinitionQuery().processDefinitionCategoryNotEquals("two").list());
        expectedProcessDefinitionNames = new HashSet<>();
        expectedProcessDefinitionNames.add("processOne");
        expectedProcessDefinitionNames.add("processThree");
        assertEquals(expectedProcessDefinitionNames, processDefinitionNames);

        repositoryService.deleteDeployment(deployment.getId());
    }

    private HashSet<String> getProcessDefinitionNames(List<ProcessDefinition> processDefinitions) {
        HashSet<String> processDefinitionNames = new HashSet<>();
        for (ProcessDefinition processDefinition : processDefinitions) {
            processDefinitionNames.add(processDefinition.getKey());
        }
        return processDefinitionNames;
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testSetProcessDefinitionCategory() {

        // Verify category and see if we can start a process instance
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertEquals("testCategory", processDefinition.getCategory());

        processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionCategory("testCategory").singleResult();
        assertNotNull(processDefinition);

        long count = runtimeService.createProcessInstanceQuery().count();
        runtimeService.startProcessInstanceById(processDefinition.getId());
        long newCount = runtimeService.createProcessInstanceQuery().count();
        assertEquals(newCount, count + 1);

        // Update category
        repositoryService.setProcessDefinitionCategory(processDefinition.getId(), "UpdatedCategory");

        assertEquals(0, repositoryService.createProcessDefinitionQuery().processDefinitionCategory("testCategory").count());
        processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionCategory("UpdatedCategory").singleResult();
        assertNotNull(processDefinition);

        // Start a process instance
        runtimeService.startProcessInstanceById(processDefinition.getId());
        newCount = runtimeService.createProcessInstanceQuery().count();
        assertEquals(newCount, count + 2);

        // Set category to null
        repositoryService.setProcessDefinitionCategory(processDefinition.getId(), null);
        assertEquals(0, repositoryService.createProcessDefinitionQuery().processDefinitionCategory("testCategory").count());
        assertEquals(0, repositoryService.createProcessDefinitionQuery().processDefinitionCategory("UpdatedCategory").count());
        assertEquals(1, repositoryService.createProcessDefinitionQuery().processDefinitionCategoryNotEquals("UpdatedCategory").count());
    }

}
