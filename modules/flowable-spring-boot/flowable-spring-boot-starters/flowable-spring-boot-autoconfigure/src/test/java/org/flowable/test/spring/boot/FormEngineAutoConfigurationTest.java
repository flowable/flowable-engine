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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.util.EngineServiceUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.form.FormEngineAutoConfiguration;
import org.flowable.spring.boot.form.FormEngineServicesAutoConfiguration;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class FormEngineAutoConfigurationTest {
    
    protected AnnotationConfigApplicationContext context;
    
    @After
    public void deleteDeployments() {
        FormRepositoryService repositoryService = context.getBean(FormRepositoryService.class);
        List<FormDeployment> formDeployments = repositoryService.createDeploymentQuery().list();
        for (FormDeployment formDeployment : formDeployments) {
            repositoryService.deleteDeployment(formDeployment.getId());
        }
    }

    @Test
    public void standaloneFormEngineWithBasicDataSource() {
        context = this.context(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            FormEngineServicesAutoConfiguration.class,
            FormEngineAutoConfiguration.class
        );

        FormEngine formEngine = context.getBean(FormEngine.class);
        assertThat(formEngine).as("Form engine").isNotNull();
        assertThat(context.getBean(FormService.class)).as("Form service")
            .isEqualTo(formEngine.getFormService());

        FormRepositoryService repositoryService = context.getBean(FormRepositoryService.class);
        assertThat(repositoryService).as("Form repository service")
            .isEqualTo(formEngine.getFormRepositoryService());

        assertThat(context.getBean(FormManagementService.class)).as("Form management service")
            .isEqualTo(formEngine.getFormManagementService());

        assertThat(context.getBean(FormEngineConfiguration.class)).as("Form engine configuration")
            .isEqualTo(formEngine.getFormEngineConfiguration());

        assertAutoDeployment(repositoryService);
    }

    @Test
    public void formEngineWithBasicDataSourceAndProcessEngine() {
        context = this.context(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            FormEngineAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlowableTransactionAutoConfiguration.class,
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class,
            FormEngineServicesAutoConfiguration.class
        );

        ProcessEngine processEngine = context.getBean(ProcessEngine.class);
        assertThat(processEngine).as("Process engine").isNotNull();
        FormEngineConfigurationApi formProcessConfigurationApi = formEngine(processEngine);

        FormEngineConfigurationApi formEngine = context.getBean(FormEngineConfigurationApi.class);
        assertThat(formEngine).isEqualTo(formProcessConfigurationApi);
        assertThat(formEngine).as("Form engine").isNotNull();
        assertThat(context.getBean(FormService.class)).as("Form service")
            .isEqualTo(formEngine.getFormService());

        FormRepositoryService repositoryService = context.getBean(FormRepositoryService.class);
        assertThat(repositoryService).as("Form repository service")
            .isEqualTo(formEngine.getFormRepositoryService());

        assertThat(context.getBean(FormManagementService.class)).as("Form management service")
            .isEqualTo(formEngine.getFormManagementService());
        assertAutoDeployment(repositoryService);
        
        List<Deployment> deployments = processEngine.getRepositoryService().createDeploymentQuery().list();
        for (Deployment deployment : deployments) {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
        
        List<FormDeployment> formDeployments = formEngine.getFormRepositoryService().createDeploymentQuery().list();
        for (FormDeployment formDeployment : formDeployments) {
            formEngine.getFormRepositoryService().deleteDeployment(formDeployment.getId());
        }
    }
    
    @Test
    public void formEngineWithBasicDataSourceAndAppEngine() {
        context = this.context(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlowableTransactionAutoConfiguration.class,
            AppEngineServicesAutoConfiguration.class,
            AppEngineAutoConfiguration.class,
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class,
            FormEngineAutoConfiguration.class,
            FormEngineServicesAutoConfiguration.class
        );

        AppEngine appEngine = context.getBean(AppEngine.class);
        assertThat(appEngine).as("App engine").isNotNull();
        FormEngineConfigurationApi formProcessConfigurationApi = formEngine(appEngine);

        FormEngineConfigurationApi formEngine = context.getBean(FormEngineConfigurationApi.class);
        assertThat(formEngine).isEqualTo(formProcessConfigurationApi);
        assertThat(formEngine).as("Form engine").isNotNull();
        assertThat(context.getBean(FormService.class)).as("Form service")
            .isEqualTo(formEngine.getFormService());

        FormRepositoryService repositoryService = context.getBean(FormRepositoryService.class);
        assertThat(repositoryService).as("Form repository service")
            .isEqualTo(formEngine.getFormRepositoryService());

        assertThat(context.getBean(FormManagementService.class)).as("Form management service")
            .isEqualTo(formEngine.getFormManagementService());
        assertAutoDeploymentWithAppEngine(repositoryService);
        
        List<AppDeployment> appDeployments = appEngine.getAppRepositoryService().createDeploymentQuery().list();
        for (AppDeployment appDeployment : appDeployments) {
            appEngine.getAppRepositoryService().deleteDeployment(appDeployment.getId(), true);
        }
        
        ProcessEngine processEngine = context.getBean(ProcessEngine.class);
        List<Deployment> deployments = processEngine.getRepositoryService().createDeploymentQuery().list();
        for (Deployment deployment : deployments) {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
        
        List<FormDeployment> formDeployments = formEngine.getFormRepositoryService().createDeploymentQuery().list();
        for (FormDeployment formDeployment : formDeployments) {
            formEngine.getFormRepositoryService().deleteDeployment(formDeployment.getId());
        }
    }

    protected void assertAutoDeployment(FormRepositoryService repositoryService) {
        List<FormDefinition> formDefinitions = repositoryService.createFormDefinitionQuery().list();
        assertThat(formDefinitions)
            .extracting(FormDefinition::getKey, FormDefinition::getName)
            .containsExactlyInAnyOrder(
                tuple("form1", "My first form")
            );
    }
    
    protected void assertAutoDeploymentWithAppEngine(FormRepositoryService repositoryService) {
        List<FormDefinition> formDefinitions = repositoryService.createFormDefinitionQuery().list();
        assertThat(formDefinitions)
            .extracting(FormDefinition::getKey, FormDefinition::getName)
            .containsExactlyInAnyOrder(
                tuple("form1", "My first form"),
                tuple("vacationInfo", "Vacation info"),
                tuple("vacationInfo", "Vacation info"),
                tuple("decimalExpressionForm77777", "Decimal expression form1"),
                tuple("decimalTestForm", "Decimal test form")
            );
        
        FormDefinition formDefinition = repositoryService.createFormDefinitionQuery().latestVersion().formDefinitionKey("vacationInfo").singleResult();
        assertThat(formDefinition.getVersion()).isEqualTo(2);
        
        formDefinition = repositoryService.createFormDefinitionQuery().latestVersion().formDefinitionKey("decimalTestForm").singleResult();
        assertThat(formDefinition.getVersion()).isOne();
        
        List<FormDeployment> deployments = repositoryService.createDeploymentQuery().list();

        assertThat(deployments).hasSize(3)
            .extracting(FormDeployment::getName)
            .containsExactlyInAnyOrder("SpringBootAutoDeployment", "simple.bar", "vacationRequest.zip");
        
        AppRepositoryService appRepositoryService = context.getBean(AppRepositoryService.class);
        List<AppDefinition> appDefinitions = appRepositoryService.createAppDefinitionQuery().list();
        
        assertThat(appDefinitions)
            .extracting(AppDefinition::getKey)
            .contains("simpleApp", "vacationRequestApp");
        
        AppDefinition appDefinition = appRepositoryService.createAppDefinitionQuery().latestVersion().appDefinitionKey("simpleApp").singleResult();
        assertThat(appDefinition.getVersion()).isOne();
        
        appDefinition = appRepositoryService.createAppDefinitionQuery().latestVersion().appDefinitionKey("vacationRequestApp").singleResult();
        assertThat(appDefinition.getVersion()).isOne();
        
        List<AppDeployment> appDeployments = appRepositoryService.createDeploymentQuery().list();
        assertThat(appDeployments).hasSize(2)
            .extracting(AppDeployment::getName)
            .contains("simple.bar", "vacationRequest.zip");
    }

    private AnnotationConfigApplicationContext context(Class<?>... clazz) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(clazz);
        annotationConfigApplicationContext.refresh();
        return annotationConfigApplicationContext;
    }

    private static FormEngineConfigurationApi formEngine(ProcessEngine processEngine) {
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        return EngineServiceUtil.getFormEngineConfiguration(processEngineConfiguration);
    }
    
    private static FormEngineConfigurationApi formEngine(AppEngine appEngine) {
        AppEngineConfiguration appEngineConfiguration = appEngine.getAppEngineConfiguration();
        return EngineServiceUtil.getFormEngineConfiguration(appEngineConfiguration);
    }
}
