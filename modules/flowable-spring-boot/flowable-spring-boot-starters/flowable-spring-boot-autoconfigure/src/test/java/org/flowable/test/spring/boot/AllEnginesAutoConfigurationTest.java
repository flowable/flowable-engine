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
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.flowable.test.spring.boot.util.DeploymentCleanerUtil.deleteDeployments;

import java.util.List;

import org.flowable.app.engine.AppEngine;
import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.impl.el.CmmnExpressionManager;
import org.flowable.cmmn.spring.SpringCmmnEngineConfiguration;
import org.flowable.cmmn.spring.configurator.SpringCmmnEngineConfigurator;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.cfg.SpringBeanFactoryProxyMap;
import org.flowable.common.engine.impl.el.DefaultExpressionManager;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.content.engine.ContentEngine;
import org.flowable.content.spring.SpringContentEngineConfiguration;
import org.flowable.content.spring.configurator.SpringContentEngineConfigurator;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.spring.SpringDmnEngineConfiguration;
import org.flowable.dmn.spring.configurator.SpringDmnEngineConfigurator;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.el.ProcessExpressionManager;
import org.flowable.engine.spring.configurator.SpringProcessEngineConfigurator;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.spring.SpringEventRegistryEngineConfiguration;
import org.flowable.eventregistry.spring.configurator.SpringEventRegistryConfigurator;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.spring.SpringFormEngineConfiguration;
import org.flowable.form.spring.configurator.SpringFormEngineConfigurator;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.idm.spring.configurator.SpringIdmEngineConfigurator;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.cmmn.CmmnEngineAutoConfiguration;
import org.flowable.spring.boot.cmmn.CmmnEngineServicesAutoConfiguration;
import org.flowable.spring.boot.content.ContentEngineAutoConfiguration;
import org.flowable.spring.boot.content.ContentEngineServicesAutoConfiguration;
import org.flowable.spring.boot.dmn.DmnEngineAutoConfiguration;
import org.flowable.spring.boot.dmn.DmnEngineServicesAutoConfiguration;
import org.flowable.spring.boot.eventregistry.EventRegistryAutoConfiguration;
import org.flowable.spring.boot.eventregistry.EventRegistryServicesAutoConfiguration;
import org.flowable.spring.boot.form.FormEngineAutoConfiguration;
import org.flowable.spring.boot.form.FormEngineServicesAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.flowable.task.api.Task;
import org.flowable.test.spring.boot.util.CustomUserEngineConfigurerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @author Filip Hrisafov
 */
public class AllEnginesAutoConfigurationTest {

    private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    DataSourceTransactionManagerAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    AppEngineServicesAutoConfiguration.class,
                    AppEngineAutoConfiguration.class,
                    IdmEngineAutoConfiguration.class,
                    IdmEngineServicesAutoConfiguration.class,
                    EventRegistryAutoConfiguration.class,
                    EventRegistryServicesAutoConfiguration.class,
                    CmmnEngineAutoConfiguration.class,
                    CmmnEngineServicesAutoConfiguration.class,
                    ContentEngineAutoConfiguration.class,
                    ContentEngineServicesAutoConfiguration.class,
                    DmnEngineAutoConfiguration.class,
                    DmnEngineServicesAutoConfiguration.class,
                    FormEngineAutoConfiguration.class,
                    FormEngineServicesAutoConfiguration.class,
                    ProcessEngineAutoConfiguration.class,
                    ProcessEngineServicesAutoConfiguration.class
            ))
            .withUserConfiguration(CustomUserEngineConfigurerConfiguration.class);

    @Test
    public void usingAllAutoConfigurationsTogetherShouldWorkCorrectly() {
        contextRunner.run(context -> {
            assertThat(context)
                    .hasSingleBean(AppEngine.class)
                    .hasSingleBean(CmmnEngine.class)
                    .hasSingleBean(ContentEngine.class)
                    .hasSingleBean(DmnEngine.class)
                    .hasSingleBean(FormEngine.class)
                    .hasSingleBean(IdmEngine.class)
                    .hasSingleBean(EventRegistryEngine.class)
                    .hasSingleBean(ProcessEngine.class)
                    .hasSingleBean(SpringAppEngineConfiguration.class)
                    .hasSingleBean(SpringCmmnEngineConfiguration.class)
                    .hasSingleBean(SpringContentEngineConfiguration.class)
                    .hasSingleBean(SpringDmnEngineConfiguration.class)
                    .hasSingleBean(SpringFormEngineConfiguration.class)
                    .hasSingleBean(SpringIdmEngineConfiguration.class)
                    .hasSingleBean(SpringEventRegistryEngineConfiguration.class)
                    .hasSingleBean(SpringProcessEngineConfiguration.class)
                    .hasSingleBean(SpringCmmnEngineConfigurator.class)
                    .hasSingleBean(SpringContentEngineConfigurator.class)
                    .hasSingleBean(SpringDmnEngineConfigurator.class)
                    .hasSingleBean(SpringFormEngineConfigurator.class)
                    .hasSingleBean(SpringIdmEngineConfigurator.class)
                    .hasSingleBean(SpringProcessEngineConfigurator.class);

            SpringAppEngineConfiguration appEngineConfiguration = context.getBean(SpringAppEngineConfiguration.class);
            SpringCmmnEngineConfiguration cmmnEngineConfiguration = context.getBean(SpringCmmnEngineConfiguration.class);
            SpringContentEngineConfiguration contentEngineConfiguration = context.getBean(SpringContentEngineConfiguration.class);
            SpringDmnEngineConfiguration dmnEngineConfiguration = context.getBean(SpringDmnEngineConfiguration.class);
            SpringFormEngineConfiguration formEngineConfiguration = context.getBean(SpringFormEngineConfiguration.class);
            SpringIdmEngineConfiguration idmEngineConfiguration = context.getBean(SpringIdmEngineConfiguration.class);
            SpringEventRegistryEngineConfiguration eventEngineConfiguration = context.getBean(SpringEventRegistryEngineConfiguration.class);
            SpringProcessEngineConfiguration processEngineConfiguration = context.getBean(SpringProcessEngineConfiguration.class);

            assertThat(appEngineConfiguration.getEngineConfigurations())
                    .as("AppEngine configurations")
                    .containsOnly(
                            entry(EngineConfigurationConstants.KEY_APP_ENGINE_CONFIG, appEngineConfiguration),
                            entry(ScopeTypes.APP, appEngineConfiguration),
                            entry(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG, cmmnEngineConfiguration),
                            entry(ScopeTypes.CMMN, cmmnEngineConfiguration),
                            entry(EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG, dmnEngineConfiguration),
                            entry(ScopeTypes.DMN, dmnEngineConfiguration),
                            entry(EngineConfigurationConstants.KEY_CONTENT_ENGINE_CONFIG, contentEngineConfiguration),
                            entry("content", contentEngineConfiguration),
                            entry(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG, formEngineConfiguration),
                            entry(ScopeTypes.FORM, formEngineConfiguration),
                            entry(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG, idmEngineConfiguration),
                            entry("idm", idmEngineConfiguration),
                            entry(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG, eventEngineConfiguration),
                            entry(ScopeTypes.EVENT_REGISTRY, eventEngineConfiguration),
                            entry(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG, processEngineConfiguration),
                            entry(ScopeTypes.BPMN, processEngineConfiguration)
                    )
                    .containsAllEntriesOf(cmmnEngineConfiguration.getEngineConfigurations())
                    .containsAllEntriesOf(dmnEngineConfiguration.getEngineConfigurations())
                    .containsAllEntriesOf(contentEngineConfiguration.getEngineConfigurations())
                    .containsAllEntriesOf(formEngineConfiguration.getEngineConfigurations())
                    .containsAllEntriesOf(idmEngineConfiguration.getEngineConfigurations())
                    .containsAllEntriesOf(eventEngineConfiguration.getEngineConfigurations())
                    .containsAllEntriesOf(processEngineConfiguration.getEngineConfigurations());

            SpringCmmnEngineConfigurator cmmnConfigurator = context.getBean(SpringCmmnEngineConfigurator.class);
            SpringContentEngineConfigurator contentConfigurator = context.getBean(SpringContentEngineConfigurator.class);
            SpringDmnEngineConfigurator dmnConfigurator = context.getBean(SpringDmnEngineConfigurator.class);
            SpringFormEngineConfigurator formConfigurator = context.getBean(SpringFormEngineConfigurator.class);
            SpringIdmEngineConfigurator idmConfigurator = context.getBean(SpringIdmEngineConfigurator.class);
            SpringEventRegistryConfigurator eventConfigurator = context.getBean(SpringEventRegistryConfigurator.class);
            SpringProcessEngineConfigurator processConfigurator = context.getBean(SpringProcessEngineConfigurator.class);
            assertThat(appEngineConfiguration.getConfigurators())
                    .as("AppEngineConfiguration configurators")
                    .containsExactly(
                            processConfigurator,
                            contentConfigurator,
                            dmnConfigurator,
                            formConfigurator,
                            cmmnConfigurator
                    );

            assertThat(cmmnEngineConfiguration.getIdmEngineConfigurator())
                    .as("CmmnEngineConfiguration idmEngineConfigurator")
                    .isNull();
            assertThat(processEngineConfiguration.getIdmEngineConfigurator())
                    .as("ProcessEngineConfiguration idmEngineConfigurator")
                    .isNull();
            assertThat(appEngineConfiguration.getIdmEngineConfigurator())
                    .as("AppEngineConfiguration idmEngineConfigurator")
                    .isSameAs(idmConfigurator);

            assertThat(appEngineConfiguration.getExpressionManager()).isInstanceOf(DefaultExpressionManager.class);
            assertThat(appEngineConfiguration.getExpressionManager().getBeans()).isInstanceOf(SpringBeanFactoryProxyMap.class);
            assertThat(processEngineConfiguration.getExpressionManager()).isNotEqualTo(appEngineConfiguration.getExpressionManager());
            assertThat(processEngineConfiguration.getExpressionManager()).isInstanceOf(ProcessExpressionManager.class);
            assertThat(processEngineConfiguration.getExpressionManager().getBeans()).isInstanceOf(SpringBeanFactoryProxyMap.class);
            assertThat(cmmnEngineConfiguration.getExpressionManager()).isNotEqualTo(appEngineConfiguration.getExpressionManager());
            assertThat(cmmnEngineConfiguration.getExpressionManager()).isInstanceOf(CmmnExpressionManager.class);
            assertThat(cmmnEngineConfiguration.getExpressionManager().getBeans()).isInstanceOf(SpringBeanFactoryProxyMap.class);
            assertThat(dmnEngineConfiguration.getExpressionManager()).isNotEqualTo(appEngineConfiguration.getExpressionManager());
            assertThat(dmnEngineConfiguration.getExpressionManager()).isInstanceOf(DefaultExpressionManager.class);
            assertThat(dmnEngineConfiguration.getExpressionManager().getBeans()).isInstanceOf(SpringBeanFactoryProxyMap.class);
            assertThat(formEngineConfiguration.getExpressionManager()).isNotEqualTo(appEngineConfiguration.getExpressionManager());
            assertThat(formEngineConfiguration.getExpressionManager()).isInstanceOf(DefaultExpressionManager.class);
            assertThat(formEngineConfiguration.getExpressionManager().getBeans()).isInstanceOf(SpringBeanFactoryProxyMap.class);

            assertThat(cmmnEngineConfiguration.isDisableEventRegistry()).isTrue();
            assertThat(cmmnEngineConfiguration.getEventRegistryConfigurator()).isNull();
            assertThat(processEngineConfiguration.isDisableEventRegistry()).isTrue();
            assertThat(processEngineConfiguration.getEventRegistryConfigurator()).isNull();
            assertThat(appEngineConfiguration.getEventRegistryConfigurator())
                    .as("AppEngineConfiguration eventEngineConfiguration")
                    .isSameAs(eventConfigurator);

            deleteDeployments(context.getBean(AppEngine.class));
            deleteDeployments(context.getBean(CmmnEngine.class));
            deleteDeployments(context.getBean(DmnEngine.class));
            deleteDeployments(context.getBean(FormEngine.class));
            deleteDeployments(context.getBean(ProcessEngine.class));
        });

    }

    @Test
    public void testInclusiveGatewayProcessTask() {
        contextRunner.run((context -> {
            SpringCmmnEngineConfiguration cmmnEngineConfiguration = context.getBean(SpringCmmnEngineConfiguration.class);
            SpringProcessEngineConfiguration processEngineConfiguration = context.getBean(SpringProcessEngineConfiguration.class);

            CmmnRuntimeService cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
            CmmnHistoryService cmmnHistoryService = cmmnEngineConfiguration.getCmmnHistoryService();
            RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();
            TaskService taskService = processEngineConfiguration.getTaskService();

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
            assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isZero();
            assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemDefinitionId("theTask")
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .list();
            assertThat(planItemInstances).hasSize(1);
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
            assertThat(runtimeService.createProcessInstanceQuery().count()).as("No process instance started").isEqualTo(1L);

            assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

            List<Task> tasks = taskService.createTaskQuery().list();
            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());

            assertThat(taskService.createTaskQuery().count()).isZero();
            assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

            planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemDefinitionId("theTask2")
                    .list();
            assertThat(planItemInstances)
                    .extracting(PlanItemInstance::getName, PlanItemInstance::getState)
                    .containsExactly(tuple("Task Two", PlanItemInstanceState.ENABLED));
        }));
    }
}
