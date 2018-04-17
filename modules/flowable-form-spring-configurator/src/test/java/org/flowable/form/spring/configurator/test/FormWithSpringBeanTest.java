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
package org.flowable.form.spring.configurator.test;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormInfo;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.model.ExpressionFormField;
import org.flowable.form.model.SimpleFormModel;
import org.flowable.task.api.Task;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Tijs Rademakers
 */
@ContextConfiguration("classpath:flowable-context.xml")
public class FormWithSpringBeanTest extends SpringFormFlowableTestCase {

    @Deployment(resources = { "org/flowable/form/spring/configurator/test/oneTaskWithFormKeyProcess.bpmn20.xml",
        "org/flowable/form/spring/configurator/test/simple.form" })
    public void testFormOnUserTask() {
        
        FormEngineConfiguration formEngineConfiguration = (FormEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                        .get(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);
        
        FormDeployment formDeployment = formEngineConfiguration.getFormRepositoryService().createDeploymentQuery().singleResult();
        assertNotNull(formDeployment);
        
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("var1", "test");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormProcess", variables);
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertNotNull(task);
            
            FormInfo formInfo = taskService.getTaskFormModel(task.getId());
            SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
            ExpressionFormField expressionFormField = (ExpressionFormField) formModel.getFields().get(1);
            assertEquals("#{testFormBean.getExpressionText(var1)}", expressionFormField.getExpression());
            assertEquals("hello test", expressionFormField.getValue());
            
            taskService.complete(task.getId());
    
            assertProcessEnded(processInstance.getId());
            
        } finally {
            formEngineConfiguration.getFormRepositoryService().deleteDeployment(formDeployment.getId());
        }
    }

}
