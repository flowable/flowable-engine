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
package org.flowable.test.spring.boot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import flowable.Application;

/**
 * Test the Spring Integration inbound inboundGateway support.
 *
 * @author Josh Long
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class IntegrationAutoConfigurationTest {

    public static final String projectId = "2143243";

    @Autowired
    private ApplicationContext applicationContext;
    @Test
    public void testLaunchingGatewayProcessDefinition() throws Exception {
        RepositoryService repositoryService = applicationContext.getBean(RepositoryService.class);
        RuntimeService runtimeService = applicationContext.getBean(RuntimeService.class);
        ProcessEngine processEngine = applicationContext.getBean(ProcessEngine.class);

        Assert.assertNotNull("the process engine should not be null", processEngine);
        Assert.assertNotNull("we should have a default repositoryService included", repositoryService);
        String integrationGatewayProcess = "integrationGatewayProcess";
        List<ProcessDefinition> processDefinitionList = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(integrationGatewayProcess)
                .list();
        ProcessDefinition processDefinition = processDefinitionList.iterator().next();
        Assert.assertEquals(integrationGatewayProcess, processDefinition.getKey());
        Map<String, Object> vars = new HashMap<>();
        vars.put("customerId", 232);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(integrationGatewayProcess, vars);
        Assert.assertNotNull("the processInstance should not be null", processInstance);
        Assert.assertEquals(projectId, applicationContext.getBean(Application.AnalysingService.class)
                .getStringAtomicReference().get());
    }
}
