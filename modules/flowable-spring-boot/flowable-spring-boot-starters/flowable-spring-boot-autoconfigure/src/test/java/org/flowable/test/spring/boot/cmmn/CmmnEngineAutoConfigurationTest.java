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
package org.flowable.test.spring.boot.cmmn;

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
import org.flowable.cmmn.api.CmmnEngineConfigurationApi;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.util.EngineServiceUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.cmmn.CmmnEngineAutoConfiguration;
import org.flowable.spring.boot.cmmn.CmmnEngineServicesAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Filip Hrisafov
 */
public class CmmnEngineAutoConfigurationTest {
    
    protected AnnotationConfigApplicationContext context;
    
    @After
    public void deleteDeployments() {
        CmmnRepositoryService repositoryService = context.getBean(CmmnRepositoryService.class);
        List<CmmnDeployment> cmmnDeployments = repositoryService.createDeploymentQuery().list();
        for (CmmnDeployment cmmnDeployment : cmmnDeployments) {
            repositoryService.deleteDeployment(cmmnDeployment.getId(), true);
        }
    }

    @Test
    public void standaloneCmmnEngineWithBasicDataSource() {
        context = this.context(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            IdmEngineAutoConfiguration.class,
            CmmnEngineServicesAutoConfiguration.class,
            CmmnEngineAutoConfiguration.class,
            IdmEngineServicesAutoConfiguration.class
        );

        CmmnEngine cmmnEngine = context.getBean(CmmnEngine.class);
        assertThat(cmmnEngine).as("Cmmn engine").isNotNull();

        assertAllServicesPresent(context, cmmnEngine);

        assertAutoDeployment(context);
    }

    @Test
    public void cmmnEngineWithBasicDataSourceAndProcessEngine() {
        context = this
            .context(
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                FlowableTransactionAutoConfiguration.class,
                CmmnEngineAutoConfiguration.class,
                IdmEngineAutoConfiguration.class,
                ProcessEngineServicesAutoConfiguration.class,
                ProcessEngineAutoConfiguration.class,
                IdmEngineServicesAutoConfiguration.class,
                CmmnEngineServicesAutoConfiguration.class
            );

        ProcessEngine processEngine = context.getBean(ProcessEngine.class);
        assertThat(processEngine).as("Process engine").isNotNull();
        CmmnEngineConfigurationApi cmmnProcessConfigurationApi = cmmnEngine(processEngine);

        CmmnEngine cmmnEngine = context.getBean(CmmnEngine.class);
        assertThat(cmmnEngine.getCmmnEngineConfiguration()).as("Cmmn Engine Configuration").isEqualTo(cmmnProcessConfigurationApi);
        assertThat(cmmnEngine).as("Cmmn engine").isNotNull();

        assertAllServicesPresent(context, cmmnEngine);
        assertAutoDeployment(context);
        
        List<Deployment> deployments = processEngine.getRepositoryService().createDeploymentQuery().list();
        for (Deployment deployment : deployments) {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
        
        List<CmmnDeployment> cmmnDeployments = cmmnEngine.getCmmnRepositoryService().createDeploymentQuery().list();
        for (CmmnDeployment cmmnDeployment : cmmnDeployments) {
            cmmnEngine.getCmmnRepositoryService().deleteDeployment(cmmnDeployment.getId(), true);
        }
    }
    
    @Test
    public void cmmnEngineWithBasicDataSourceAndAppEngine() {
        context = this
            .context(
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                FlowableTransactionAutoConfiguration.class,
                AppEngineServicesAutoConfiguration.class,
                AppEngineAutoConfiguration.class,
                ProcessEngineServicesAutoConfiguration.class,
                ProcessEngineAutoConfiguration.class,
                CmmnEngineAutoConfiguration.class,
                IdmEngineAutoConfiguration.class,
                IdmEngineServicesAutoConfiguration.class,
                CmmnEngineServicesAutoConfiguration.class
            );

        AppEngine appEngine = context.getBean(AppEngine.class);
        assertThat(appEngine).as("App engine").isNotNull();
        CmmnEngineConfigurationApi cmmnProcessConfigurationApi = cmmnEngine(appEngine);

        CmmnEngine cmmnEngine = context.getBean(CmmnEngine.class);
        assertThat(cmmnEngine.getCmmnEngineConfiguration()).as("Cmmn Engine Configuration").isEqualTo(cmmnProcessConfigurationApi);
        assertThat(cmmnEngine).as("Cmmn engine").isNotNull();

        assertAllServicesPresent(context, cmmnEngine);
        assertAutoDeploymentWithAppEngine(context);
        
        List<AppDeployment> appDeployments = appEngine.getAppRepositoryService().createDeploymentQuery().list();
        for (AppDeployment appDeployment : appDeployments) {
            appEngine.getAppRepositoryService().deleteDeployment(appDeployment.getId(), true);
        }
        
        ProcessEngine processEngine = context.getBean(ProcessEngine.class);
        List<Deployment> deployments = processEngine.getRepositoryService().createDeploymentQuery().list();
        for (Deployment deployment : deployments) {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
        
        List<CmmnDeployment> cmmnDeployments = cmmnEngine.getCmmnRepositoryService().createDeploymentQuery().list();
        for (CmmnDeployment cmmnDeployment : cmmnDeployments) {
            cmmnEngine.getCmmnRepositoryService().deleteDeployment(cmmnDeployment.getId(), true);
        }
    }
    
    private void assertAllServicesPresent(AnnotationConfigApplicationContext context, CmmnEngine cmmnEngine) {
        List<Method> methods = Stream.of(CmmnEngine.class.getDeclaredMethods())
            .filter(method -> !(method.getName().equals("close") || method.getName().equals("getName"))).collect(Collectors.toList());

        assertThat(methods).allSatisfy(method -> {
            try {
                assertThat(context.getBean(method.getReturnType())).as(method.getReturnType() + " bean").isEqualTo(method.invoke(cmmnEngine));
            } catch (IllegalAccessException | InvocationTargetException e) {
                fail("Failed to invoke method " + method, e);
            }
        });
    }
    
    private void assertAutoDeployment(AnnotationConfigApplicationContext context) {
        CmmnRepositoryService repositoryService = context.getBean(CmmnRepositoryService.class);

        List<CaseDefinition> caseDefinitions = repositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().asc().list();
        assertThat(caseDefinitions)
            .extracting(CaseDefinition::getKey)
            .containsExactly("case1", "case2", "case3", "case4");
        List<CmmnDeployment> deployments = repositoryService.createDeploymentQuery().list();

        assertThat(deployments)
            .hasSize(1)
            .first()
            .satisfies(deployment -> assertThat(deployment.getName()).isEqualTo("SpringBootAutoDeployment"));
    }
    
    private void assertAutoDeploymentWithAppEngine(AnnotationConfigApplicationContext context) {
        CmmnRepositoryService repositoryService = context.getBean(CmmnRepositoryService.class);

        List<CaseDefinition> caseDefinitions = repositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().asc().list();
        assertThat(caseDefinitions)
            .extracting(CaseDefinition::getKey)
            .contains("case1", "case2", "case3", "case4", "caseB");
        
        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().latestVersion().caseDefinitionKey("case2").singleResult();
        assertThat(caseDefinition.getVersion()).isOne();
        
        caseDefinition = repositoryService.createCaseDefinitionQuery().latestVersion().caseDefinitionKey("caseB").singleResult();
        assertThat(caseDefinition.getVersion()).isOne();
        
        List<CmmnDeployment> deployments = repositoryService.createDeploymentQuery().list();

        assertThat(deployments).hasSize(2)
            .extracting(CmmnDeployment::getName)
            .contains("SpringBootAutoDeployment", "simple.bar");
        
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

    private static CmmnEngineConfigurationApi cmmnEngine(ProcessEngine processEngine) {
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        return EngineServiceUtil.getCmmnEngineConfiguration(processEngineConfiguration);
    }
    
    private static CmmnEngineConfigurationApi cmmnEngine(AppEngine appEngine) {
        AppEngineConfiguration appEngineConfiguration = appEngine.getAppEngineConfiguration();
        return EngineServiceUtil.getCmmnEngineConfiguration(appEngineConfiguration);
    }
}
