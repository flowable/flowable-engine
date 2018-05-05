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
package org.flowable.test.spring.boot.dmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;

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
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnEngineConfigurationApi;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.util.EngineServiceUtil;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.dmn.DmnEngineAutoConfiguration;
import org.flowable.spring.boot.dmn.DmnEngineServicesAutoConfiguration;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class DmnEngineAutoConfigurationTest {
    
    protected AnnotationConfigApplicationContext context;
    
    @After
    public void deleteDeployments() {
        DmnRepositoryService repositoryService = context.getBean(DmnRepositoryService.class);
        List<DmnDeployment> dmnDeployments = repositoryService.createDeploymentQuery().list();
        for (DmnDeployment dmnDeployment : dmnDeployments) {
            repositoryService.deleteDeployment(dmnDeployment.getId());
        }
    }

    @Test
    public void standaloneDmnEngineWithBasicDataSource() {
        context = this.context(DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
                        DmnEngineServicesAutoConfiguration.class, DmnEngineAutoConfiguration.class);

        DmnEngine dmnEngine = context.getBean(DmnEngine.class);
        assertThat(dmnEngine).as("Dmn engine").isNotNull();

        assertAllServicesPresent(context, dmnEngine);
        assertAutoDeployment(context.getBean(DmnRepositoryService.class));
    }

    @Test
    public void dmnEngineWithBasicDataSourceAndProcessEngine() {
        context = this.context(
                DataSourceAutoConfiguration.class, 
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class, 
                FlowableTransactionAutoConfiguration.class, 
                ProcessEngineServicesAutoConfiguration.class, 
                ProcessEngineAutoConfiguration.class, 
                DmnEngineAutoConfiguration.class, 
                DmnEngineServicesAutoConfiguration.class);

        ProcessEngine processEngine = context.getBean(ProcessEngine.class);
        assertThat(processEngine).as("Process engine").isNotNull();
        DmnEngineConfigurationApi dmnProcessConfigurationApi = dmnEngine(processEngine);

        DmnEngine dmnEngine = context.getBean(DmnEngine.class);
        assertThat(dmnEngine.getDmnEngineConfiguration()).as("Dmn Engine Configuration").isEqualTo(dmnProcessConfigurationApi);
        assertThat(dmnEngine).as("Dmn engine").isNotNull();

        assertAllServicesPresent(context, dmnEngine);
        assertAutoDeployment(context.getBean(DmnRepositoryService.class));
    }
    
    @Test
    public void dmnEngineWithBasicDataSourceAndAppEngine() {
        context = this.context(
                DataSourceAutoConfiguration.class, 
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class, 
                FlowableTransactionAutoConfiguration.class, 
                AppEngineServicesAutoConfiguration.class, 
                AppEngineAutoConfiguration.class, 
                ProcessEngineServicesAutoConfiguration.class, 
                ProcessEngineAutoConfiguration.class, 
                DmnEngineAutoConfiguration.class, 
                DmnEngineServicesAutoConfiguration.class);

        AppEngine appEngine = context.getBean(AppEngine.class);
        assertThat(appEngine).as("app engine").isNotNull();
        DmnEngineConfigurationApi dmnProcessConfigurationApi = dmnEngine(appEngine);

        DmnEngine dmnEngine = context.getBean(DmnEngine.class);
        assertThat(dmnEngine.getDmnEngineConfiguration()).as("Dmn Engine Configuration").isEqualTo(dmnProcessConfigurationApi);
        assertThat(dmnEngine).as("Dmn engine").isNotNull();

        assertAllServicesPresent(context, dmnEngine);
        assertAutoDeploymentWithAppEngine(context.getBean(DmnRepositoryService.class));
    }
    
    private void assertAllServicesPresent(AnnotationConfigApplicationContext context, DmnEngine dmnEngine) {
        List<Method> methods = Stream.of(DmnEngine.class.getDeclaredMethods())
                        .filter(method -> !(method.getName().equals("close") || method.getName().equals("getName"))).collect(Collectors.toList());

        assertThat(methods).allSatisfy(method -> {
            try {
                assertThat(context.getBean(method.getReturnType())).as(method.getReturnType() + " bean").isEqualTo(method.invoke(dmnEngine));
            } catch (IllegalAccessException | InvocationTargetException e) {
                fail("Failed to invoke method " + method, e);
            }
        });
    }

    protected void assertAutoDeployment(DmnRepositoryService repositoryService) {
        List<DmnDecisionTable> decisions = repositoryService.createDecisionTableQuery().list();
        assertThat(decisions)
            .extracting(DmnDecisionTable::getKey, DmnDecisionTable::getName)
            .containsExactlyInAnyOrder(
                tuple("RiskRating", "Risk Rating Decision Table"),
                tuple("simple", "Full Decision"),
                tuple("strings1", "Simple decision"),
                tuple("strings2", "Simple decision")
            );
    }
    
    protected void assertAutoDeploymentWithAppEngine(DmnRepositoryService repositoryService) {
        List<DmnDecisionTable> decisions = repositoryService.createDecisionTableQuery().list();
        assertThat(decisions)
            .extracting(DmnDecisionTable::getKey, DmnDecisionTable::getName)
            .containsExactlyInAnyOrder(
                tuple("RiskRating", "Risk Rating Decision Table"),
                tuple("simple", "Full Decision"),
                tuple("strings1", "Simple decision"),
                tuple("strings2", "Simple decision"),
                tuple("managerApprovalNeeded", "Manager approval needed2")
            );
        
        DmnDecisionTable dmnDecisionTable = repositoryService.createDecisionTableQuery().latestVersion().decisionTableKey("strings1").singleResult();
        assertThat(dmnDecisionTable.getVersion()).isOne();
        
        dmnDecisionTable = repositoryService.createDecisionTableQuery().latestVersion().decisionTableKey("managerApprovalNeeded").singleResult();
        assertThat(dmnDecisionTable.getVersion()).isOne();
        
        List<DmnDeployment> deployments = repositoryService.createDeploymentQuery().list();

        assertThat(deployments).hasSize(2)
            .extracting(DmnDeployment::getName)
            .containsExactlyInAnyOrder("SpringBootAutoDeployment", "vacationRequest.zip");
        
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
            .containsExactlyInAnyOrder("simple.bar", "vacationRequest.zip");
    }

    private AnnotationConfigApplicationContext context(Class<?>... clazz) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(clazz);
        annotationConfigApplicationContext.refresh();
        return annotationConfigApplicationContext;
    }

    private static DmnEngineConfigurationApi dmnEngine(ProcessEngine processEngine) {
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        return EngineServiceUtil.getDmnEngineConfiguration(processEngineConfiguration);
    }
    
    private static DmnEngineConfigurationApi dmnEngine(AppEngine appEngine) {
        AppEngineConfiguration appEngineConfiguration = appEngine.getAppEngineConfiguration();
        return EngineServiceUtil.getDmnEngineConfiguration(appEngineConfiguration);
    }
}
