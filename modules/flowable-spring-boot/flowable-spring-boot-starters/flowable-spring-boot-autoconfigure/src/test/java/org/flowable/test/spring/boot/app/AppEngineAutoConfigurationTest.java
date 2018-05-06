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
package org.flowable.test.spring.boot.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.flowable.task.api.Task;
import org.junit.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Tijs Rademakers
 */
public class AppEngineAutoConfigurationTest {

    @Test
    public void standaloneAppEngineWithBasicDatasource() {
        AnnotationConfigApplicationContext context = this
            .context(
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                FlowableTransactionAutoConfiguration.class,
                AppEngineServicesAutoConfiguration.class,
                AppEngineAutoConfiguration.class,
                IdmEngineAutoConfiguration.class,
                IdmEngineServicesAutoConfiguration.class
            );
        
        AppEngine appEngine = context.getBean(AppEngine.class);
        assertThat(appEngine).as("App engine").isNotNull();

        assertAllServicesPresent(context, appEngine);
        assertAutoDeployment(context);
        
        List<AppDeployment> appDeployments = appEngine.getAppRepositoryService().createDeploymentQuery().list();
        for (AppDeployment appDeployment : appDeployments) {
            appEngine.getAppRepositoryService().deleteDeployment(appDeployment.getId(), true);
        }
    }
    
    @Test
    public void appEngineWithBasicDataSourceAndProcessEngine() {
        AnnotationConfigApplicationContext context = this
            .context(
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                FlowableTransactionAutoConfiguration.class,
                AppEngineServicesAutoConfiguration.class,
                AppEngineAutoConfiguration.class,
                ProcessEngineServicesAutoConfiguration.class,
                ProcessEngineAutoConfiguration.class,
                IdmEngineAutoConfiguration.class,
                IdmEngineServicesAutoConfiguration.class
            );

        AppEngine appEngine = context.getBean(AppEngine.class);
        assertThat(appEngine).as("App engine").isNotNull();
        ProcessEngineConfiguration processConfiguration = processEngine(appEngine);

        ProcessEngine processEngine = context.getBean(ProcessEngine.class);
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        assertThat(processEngineConfiguration).as("Proccess Engine Configuration").isEqualTo(processConfiguration);
        assertThat(processEngine).as("Process engine").isNotNull();

        assertAllServicesPresent(context, appEngine);
        assertAutoDeployment(context);
        
        processEngineConfiguration.getIdentityService().setAuthenticatedUserId("test");
        ProcessInstance processInstance = processEngineConfiguration.getRuntimeService().startProcessInstanceByKey("vacationRequest");
        Task task = processEngineConfiguration.getTaskService().createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        List<AppDeployment> appDeployments = appEngine.getAppRepositoryService().createDeploymentQuery().list();
        for (AppDeployment appDeployment : appDeployments) {
            appEngine.getAppRepositoryService().deleteDeployment(appDeployment.getId(), true);
        }
        
        List<Deployment> deployments = processEngine.getRepositoryService().createDeploymentQuery().list();
        for (Deployment deployment : deployments) {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }

    private void assertAllServicesPresent(ApplicationContext context, AppEngine appEngine) {
        List<Method> methods = Stream.of(AppEngine.class.getDeclaredMethods())
            .filter(method -> !(method.getName().equals("close") || method.getName().equals("getName"))).collect(Collectors.toList());

        assertThat(methods).allSatisfy(method -> {
            try {
                assertThat(context.getBean(method.getReturnType())).as(method.getReturnType() + " bean").isEqualTo(method.invoke(appEngine));
            } catch (IllegalAccessException | InvocationTargetException e) {
                fail("Failed to invoke method " + method, e);
            }
        });
    }

    private void assertAutoDeployment(ApplicationContext context) {
        AppRepositoryService appRepositoryService = context.getBean(AppRepositoryService.class);

        List<AppDefinition> definitions = appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionKey().asc().list();
        assertThat(definitions)
            .extracting(AppDefinition::getKey)
            .containsExactlyInAnyOrder("simpleApp", "vacationRequestApp");
        List<AppDeployment> deployments = appRepositoryService.createDeploymentQuery().orderByDeploymentName().asc().list();

        assertThat(deployments)
            .hasSize(2)
            .first()
            .satisfies(deployment -> assertThat(deployment.getName()).isEqualTo("simple.bar"));
    }
    
    private static ProcessEngineConfiguration processEngine(AppEngine appEngine) {
        AppEngineConfiguration appEngineConfiguration = appEngine.getAppEngineConfiguration();
        return (ProcessEngineConfiguration) appEngineConfiguration.getEngineConfigurations().get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
    }

    private AnnotationConfigApplicationContext context(Class<?>... clazz) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(clazz);
        annotationConfigApplicationContext.refresh();
        return annotationConfigApplicationContext;
    }
}
