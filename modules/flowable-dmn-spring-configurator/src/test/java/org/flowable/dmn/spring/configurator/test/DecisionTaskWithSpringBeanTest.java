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
package org.flowable.dmn.spring.configurator.test;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Tijs Rademakers
 */
@ContextConfiguration("classpath:flowable-context.xml")
public class DecisionTaskWithSpringBeanTest extends SpringDmnFlowableTestCase {

    @Deployment(resources = { "org/flowable/dmn/spring/configurator/test/oneDecisionTaskProcess.bpmn20.xml",
        "org/flowable/dmn/spring/configurator/test/simple.dmn" })
    public void testDecisionTask() {
        
        DmnEngineConfiguration dmnEngineConfiguration = (DmnEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                        .get(EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG);
        
        DmnDeployment dmnDeployment = dmnEngineConfiguration.getDmnRepositoryService().createDeploymentQuery().singleResult();
        assertNotNull(dmnDeployment);
        
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("input1", "testBla");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneDecisionTaskProcess", variables);
    
            assertProcessEnded(processInstance.getId());
            
        } finally {
            dmnEngineConfiguration.getDmnRepositoryService().deleteDeployment(dmnDeployment.getId());
        }
    }

}
