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
import static org.flowable.test.spring.boot.util.DeploymentCleanerUtil.deleteDeployments;

import java.util.List;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.util.EngineServiceUtil;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.spring.SpringFormEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.form.FormEngineAutoConfiguration;
import org.flowable.spring.boot.form.FormEngineServicesAutoConfiguration;
import org.flowable.test.spring.boot.util.CustomUserEngineConfigurerConfiguration;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class FormEngineAutoConfigurationTest {

    private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            FormEngineServicesAutoConfiguration.class,
            FormEngineAutoConfiguration.class
        ))
        .withUserConfiguration(CustomUserEngineConfigurerConfiguration.class);

    @Test
    public void standaloneFormEngineWithBasicDataSource() {
        contextRunner.run(context -> {
            assertThat(context)
                .doesNotHaveBean(AppEngine.class)
                .doesNotHaveBean(ProcessEngine.class)
                .doesNotHaveBean("formProcessEngineConfigurationConfigurer")
                .doesNotHaveBean("formAppEngineConfigurationConfigurer");
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

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactly(
                            SpringFormEngineConfiguration.class
                        );
                });

            deleteDeployments(formEngine);
        });

    }

    @Test
    public void formEngineWithBasicDataSourceAndProcessEngine() {
        contextRunner.withConfiguration(AutoConfigurations.of(
            HibernateJpaAutoConfiguration.class,
            FlowableTransactionAutoConfiguration.class,
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class
        )).run(context -> {
            assertThat(context)
                .doesNotHaveBean(AppEngine.class)
                .hasBean("formProcessEngineConfigurationConfigurer")
                .doesNotHaveBean("formAppEngineConfigurationConfigurer");
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

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactly(
                            SpringFormEngineConfiguration.class,
                            SpringProcessEngineConfiguration.class
                        );
                });

            deleteDeployments(processEngine);
            deleteDeployments(context.getBean(FormEngine.class));
        });
    }
    
    @Test
    public void formEngineWithBasicDataSourceAndAppEngine() {
        contextRunner.withConfiguration(AutoConfigurations.of(
            HibernateJpaAutoConfiguration.class,
            FlowableTransactionAutoConfiguration.class,
            AppEngineServicesAutoConfiguration.class,
            AppEngineAutoConfiguration.class,
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class
        )).run(context -> {
            assertThat(context)
                .doesNotHaveBean("formProcessEngineConfigurationConfigurer")
                .hasBean("formAppEngineConfigurationConfigurer");
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
            assertAutoDeploymentWithAppEngine(context);

            assertThat(context).hasSingleBean(CustomUserEngineConfigurerConfiguration.class)
                .getBean(CustomUserEngineConfigurerConfiguration.class)
                .satisfies(configuration -> {
                    assertThat(configuration.getInvokedConfigurations())
                        .containsExactly(
                            SpringProcessEngineConfiguration.class,
                            SpringFormEngineConfiguration.class,
                            SpringAppEngineConfiguration.class
                        );
                });

            deleteDeployments(appEngine);
            deleteDeployments(context.getBean(ProcessEngine.class));
            deleteDeployments(context.getBean(FormEngine.class));
        });
    }

    protected void assertAutoDeployment(FormRepositoryService repositoryService) {
        List<FormDefinition> formDefinitions = repositoryService.createFormDefinitionQuery().list();
        assertThat(formDefinitions)
            .extracting(FormDefinition::getKey, FormDefinition::getName)
            .containsExactlyInAnyOrder(
                tuple("form1", "My first form")
            );
    }

    protected void assertAutoDeploymentWithAppEngine(ApplicationContext context) {
        FormRepositoryService repositoryService = context.getBean(FormRepositoryService.class);
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
