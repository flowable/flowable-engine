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

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(processDefinitionNames).isEqualTo(expectedProcessDefinitionNames);

        processDefinitionNames = getProcessDefinitionNames(repositoryService.createProcessDefinitionQuery().processDefinitionCategoryNotEquals("two").list());
        expectedProcessDefinitionNames = new HashSet<>();
        expectedProcessDefinitionNames.add("processOne");
        expectedProcessDefinitionNames.add("processThree");
        assertThat(processDefinitionNames).isEqualTo(expectedProcessDefinitionNames);

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
        assertThat(processDefinition.getCategory()).isEqualTo("testCategory");

        processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionCategory("testCategory").singleResult();
        assertThat(processDefinition).isNotNull();

        long count = runtimeService.createProcessInstanceQuery().count();
        runtimeService.startProcessInstanceById(processDefinition.getId());
        long newCount = runtimeService.createProcessInstanceQuery().count();
        assertThat(count + 1).isEqualTo(newCount);

        // Update category
        repositoryService.setProcessDefinitionCategory(processDefinition.getId(), "UpdatedCategory");

        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionCategory("testCategory").count()).isZero();
        processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionCategory("UpdatedCategory").singleResult();
        assertThat(processDefinition).isNotNull();

        // Start a process instance
        runtimeService.startProcessInstanceById(processDefinition.getId());
        newCount = runtimeService.createProcessInstanceQuery().count();
        assertThat(count + 2).isEqualTo(newCount);

        // Set category to null
        repositoryService.setProcessDefinitionCategory(processDefinition.getId(), null);
        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionCategory("testCategory").count()).isZero();
        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionCategory("UpdatedCategory").count()).isZero();
        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionCategoryNotEquals("UpdatedCategory").count()).isEqualTo(1);
    }

}
