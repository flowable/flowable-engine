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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.model.ExpressionFormField;
import org.flowable.form.model.SimpleFormModel;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
@ContextConfiguration("classpath:flowable-context.xml")
public class FormWithSpringBeanTest extends SpringFormFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/form/spring/configurator/test/oneTaskWithFormKeyProcess.bpmn20.xml",
            "org/flowable/form/spring/configurator/test/simple.form" })
    public void testFormOnUserTask() {

        FormEngineConfiguration formEngineConfiguration = (FormEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);

        FormRepositoryService formRepositoryService = formEngineConfiguration.getFormRepositoryService();
        FormDeployment formDeployment = formRepositoryService.createDeploymentQuery().singleResult();
        assertThat(formDeployment).isNotNull();

        Set<String> formDeploymentIds = new HashSet<>();
        formDeploymentIds.add(formDeployment.getId());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("var1", "test");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormProcess", variables);
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            FormInfo formInfo = taskService.getTaskFormModel(task.getId());
            assertThat(formInfo.getName()).isEqualTo("My first form");
            SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
            ExpressionFormField expressionFormField = (ExpressionFormField) formModel.getFields().get(1);
            assertThat(expressionFormField.getExpression()).isEqualTo("#{testFormBean.getExpressionText(var1)}");
            assertThat(expressionFormField.getValue()).isEqualTo("hello test");

            // deploying new Form key should return the same deployment form

            String formV2DeploymentId = formRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/form/spring/configurator/test/simpleV2.form")
                    .deploy()
                    .getId();
            formDeploymentIds.add(formV2DeploymentId);

            formInfo = taskService.getTaskFormModel(task.getId());
            assertThat(formInfo.getName()).isEqualTo("My first form");
            formModel = (SimpleFormModel) formInfo.getFormModel();
            expressionFormField = (ExpressionFormField) formModel.getFields().get(1);
            assertThat(expressionFormField.getExpression()).isEqualTo("#{testFormBean.getExpressionText(var1)}");
            assertThat(expressionFormField.getValue()).isEqualTo("hello test");

            taskService.complete(task.getId());

            assertProcessEnded(processInstance.getId());

        } finally {
            formDeploymentIds.forEach(id -> formRepositoryService.deleteDeployment(id, true));
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/form/spring/configurator/test/oneTaskWithFormKeyProcessSameDeploymentFalse.bpmn20.xml",
            "org/flowable/form/spring/configurator/test/simple.form"
    })
    public void testFormOnUserTaskSameDeploymentFalse() {

        FormEngineConfiguration formEngineConfiguration = (FormEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);

        FormRepositoryService formRepositoryService = formEngineConfiguration.getFormRepositoryService();
        FormDeployment formDeployment = formRepositoryService.createDeploymentQuery().singleResult();
        assertThat(formDeployment).isNotNull();

        Set<String> formDeploymentIds = new HashSet<>();
        formDeploymentIds.add(formDeployment.getId());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("var1", "test");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormProcess", variables);
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            FormInfo formInfo = taskService.getTaskFormModel(task.getId());
            assertThat(formInfo.getName()).isEqualTo("My first form");
            SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
            ExpressionFormField expressionFormField = (ExpressionFormField) formModel.getFields().get(1);
            assertThat(expressionFormField.getExpression()).isEqualTo("#{testFormBean.getExpressionText(var1)}");
            assertThat(expressionFormField.getValue()).isEqualTo("hello test");

            // deploying new Form key should return the new form

            String formV2DeploymentId = formRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/form/spring/configurator/test/simpleV2.form")
                    .deploy()
                    .getId();
            formDeploymentIds.add(formV2DeploymentId);

            formInfo = taskService.getTaskFormModel(task.getId());
            assertThat(formInfo.getName()).isEqualTo("My second form");
            formModel = (SimpleFormModel) formInfo.getFormModel();
            expressionFormField = (ExpressionFormField) formModel.getFields().get(1);
            assertThat(expressionFormField.getExpression()).isEqualTo("#{testFormBean.getExpressionText(var1).concat(' V2')}");
            assertThat(expressionFormField.getValue()).isEqualTo("hello test V2");

            taskService.complete(task.getId());

            assertProcessEnded(processInstance.getId());

        } finally {
            formDeploymentIds.forEach(id -> formRepositoryService.deleteDeployment(id, true));
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/form/spring/configurator/test/oneTaskWithFormKeyProcess.bpmn20.xml",
            "org/flowable/form/spring/configurator/test/simple.form" })
    public void testFormOnUserTaskWithoutVariables() {

        FormEngineConfiguration formEngineConfiguration = (FormEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);

        FormRepositoryService formRepositoryService = formEngineConfiguration.getFormRepositoryService();
        FormDeployment formDeployment = formRepositoryService.createDeploymentQuery().singleResult();
        assertThat(formDeployment).isNotNull();

        Set<String> formDeploymentIds = new HashSet<>();
        formDeploymentIds.add(formDeployment.getId());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("var1", "test");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormProcess", variables);
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            FormInfo formInfo = taskService.getTaskFormModel(task.getId(), true);
            assertThat(formInfo.getName()).isEqualTo("My first form");
            SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
            ExpressionFormField expressionFormField = (ExpressionFormField) formModel.getFields().get(1);
            assertThat(expressionFormField.getExpression()).isEqualTo("#{testFormBean.getExpressionText(var1)}");
            assertThat(expressionFormField.getValue()).isNull();

            // deploying new Form key should return the same deployment form

            String formV2DeploymentId = formRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/form/spring/configurator/test/simpleV2.form")
                    .deploy()
                    .getId();
            formDeploymentIds.add(formV2DeploymentId);

            formInfo = taskService.getTaskFormModel(task.getId(), true);
            assertThat(formInfo.getName()).isEqualTo("My first form");
            formModel = (SimpleFormModel) formInfo.getFormModel();
            expressionFormField = (ExpressionFormField) formModel.getFields().get(1);
            assertThat(expressionFormField.getExpression()).isEqualTo("#{testFormBean.getExpressionText(var1)}");
            assertThat(expressionFormField.getValue()).isNull();

        } finally {
            formDeploymentIds.forEach(id -> formRepositoryService.deleteDeployment(id, true));
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/form/spring/configurator/test/oneTaskWithFormKeyProcessSameDeploymentFalse.bpmn20.xml",
            "org/flowable/form/spring/configurator/test/simple.form"
    })
    public void testFormOnUserTaskWithoutVariablesSameDeploymentFalse() {

        FormEngineConfiguration formEngineConfiguration = (FormEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);

        FormRepositoryService formRepositoryService = formEngineConfiguration.getFormRepositoryService();
        FormDeployment formDeployment = formRepositoryService.createDeploymentQuery().singleResult();
        assertThat(formDeployment).isNotNull();

        Set<String> formDeploymentIds = new HashSet<>();
        formDeploymentIds.add(formDeployment.getId());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("var1", "test");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormProcess", variables);
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            FormInfo formInfo = taskService.getTaskFormModel(task.getId(), true);
            assertThat(formInfo.getName()).isEqualTo("My first form");
            SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
            ExpressionFormField expressionFormField = (ExpressionFormField) formModel.getFields().get(1);
            assertThat(expressionFormField.getExpression()).isEqualTo("#{testFormBean.getExpressionText(var1)}");
            assertThat(expressionFormField.getValue()).isNull();

            // deploying new Form key should return the same deployment form

            String formV2DeploymentId = formRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/form/spring/configurator/test/simpleV2.form")
                    .deploy()
                    .getId();
            formDeploymentIds.add(formV2DeploymentId);

            formInfo = taskService.getTaskFormModel(task.getId(), true);
            assertThat(formInfo.getName()).isEqualTo("My second form");
            formModel = (SimpleFormModel) formInfo.getFormModel();
            expressionFormField = (ExpressionFormField) formModel.getFields().get(1);
            assertThat(expressionFormField.getExpression()).isEqualTo("#{testFormBean.getExpressionText(var1).concat(' V2')}");
            assertThat(expressionFormField.getValue()).isNull();

        } finally {
            formDeploymentIds.forEach(id -> formRepositoryService.deleteDeployment(id, true));
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/form/spring/configurator/test/oneTaskWithFormKeyProcess.bpmn20.xml" })
    public void testFormOnUserTaskWithoutVariablesSeparateDeployments() {

        FormEngineConfiguration formEngineConfiguration = (FormEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);

        FormRepositoryService formRepositoryService = formEngineConfiguration.getFormRepositoryService();
        FormDeployment formDeployment = formRepositoryService.createDeployment().addClasspathResource("org/flowable/form/spring/configurator/test/simple.form")
                .deploy();

        Set<String> formDeploymentIds = new HashSet<>();
        formDeploymentIds.add(formDeployment.getId());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("var1", "test");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormProcess", variables);
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            FormInfo formInfo = taskService.getTaskFormModel(task.getId(), true);
            SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
            ExpressionFormField expressionFormField = (ExpressionFormField) formModel.getFields().get(1);
            assertThat(expressionFormField.getExpression()).isEqualTo("#{testFormBean.getExpressionText(var1)}");
            assertThat(expressionFormField.getValue()).isNull();

            // deploying new Form key should return the new form

            String formV2DeploymentId = formRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/form/spring/configurator/test/simpleV2.form")
                    .deploy()
                    .getId();
            formDeploymentIds.add(formV2DeploymentId);

            formInfo = taskService.getTaskFormModel(task.getId(), true);
            assertThat(formInfo.getName()).isEqualTo("My second form");
            formModel = (SimpleFormModel) formInfo.getFormModel();
            expressionFormField = (ExpressionFormField) formModel.getFields().get(1);
            assertThat(expressionFormField.getExpression()).isEqualTo("#{testFormBean.getExpressionText(var1).concat(' V2')}");

        } finally {
            formDeploymentIds.forEach(id -> formRepositoryService.deleteDeployment(id, true));
        }
    }

}
