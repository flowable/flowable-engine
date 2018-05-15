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
import static org.flowable.test.spring.boot.util.DeploymentCleanerUtil.deleteDeployments;

import org.flowable.app.engine.AppEngine;
import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.app.spring.SpringAppExpressionManager;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.spring.SpringCmmnEngineConfiguration;
import org.flowable.cmmn.spring.SpringCmmnExpressionManager;
import org.flowable.cmmn.spring.configurator.SpringCmmnEngineConfigurator;
import org.flowable.common.engine.impl.cfg.SpringBeanFactoryProxyMap;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.content.engine.ContentEngine;
import org.flowable.content.spring.SpringContentEngineConfiguration;
import org.flowable.content.spring.configurator.SpringContentEngineConfigurator;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.spring.SpringDmnEngineConfiguration;
import org.flowable.dmn.spring.SpringDmnExpressionManager;
import org.flowable.dmn.spring.configurator.SpringDmnEngineConfigurator;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.spring.configurator.SpringProcessEngineConfigurator;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.spring.SpringFormEngineConfiguration;
import org.flowable.form.spring.SpringFormExpressionManager;
import org.flowable.form.spring.configurator.SpringFormEngineConfigurator;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.idm.spring.configurator.SpringIdmEngineConfigurator;
import org.flowable.spring.SpringExpressionManager;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
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
import org.flowable.spring.boot.form.FormEngineAutoConfiguration;
import org.flowable.spring.boot.form.FormEngineServicesAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.flowable.test.spring.boot.util.CustomUserEngineConfigurerConfiguration;
import org.junit.Test;
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
            FlowableTransactionAutoConfiguration.class,
            AppEngineServicesAutoConfiguration.class,
            AppEngineAutoConfiguration.class,
            IdmEngineAutoConfiguration.class,
            IdmEngineServicesAutoConfiguration.class,
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
                .hasSingleBean(ProcessEngine.class)
                .hasSingleBean(SpringAppEngineConfiguration.class)
                .hasSingleBean(SpringCmmnEngineConfiguration.class)
                .hasSingleBean(SpringContentEngineConfiguration.class)
                .hasSingleBean(SpringDmnEngineConfiguration.class)
                .hasSingleBean(SpringFormEngineConfiguration.class)
                .hasSingleBean(SpringIdmEngineConfiguration.class)
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
            SpringProcessEngineConfiguration processEngineConfiguration = context.getBean(SpringProcessEngineConfiguration.class);

            assertThat(appEngineConfiguration.getEngineConfigurations())
                .as("AppEngine configurations")
                .containsOnly(
                    entry(EngineConfigurationConstants.KEY_APP_ENGINE_CONFIG, appEngineConfiguration),
                    entry(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG, cmmnEngineConfiguration),
                    entry(EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG, dmnEngineConfiguration),
                    entry(EngineConfigurationConstants.KEY_CONTENT_ENGINE_CONFIG, contentEngineConfiguration),
                    entry(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG, formEngineConfiguration),
                    entry(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG, idmEngineConfiguration),
                    entry(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG, processEngineConfiguration)
                )
                .containsAllEntriesOf(cmmnEngineConfiguration.getEngineConfigurations())
                .containsAllEntriesOf(dmnEngineConfiguration.getEngineConfigurations())
                .containsAllEntriesOf(contentEngineConfiguration.getEngineConfigurations())
                .containsAllEntriesOf(formEngineConfiguration.getEngineConfigurations())
                .containsAllEntriesOf(idmEngineConfiguration.getEngineConfigurations())
                .containsAllEntriesOf(processEngineConfiguration.getEngineConfigurations());

            SpringCmmnEngineConfigurator cmmnConfigurator = context.getBean(SpringCmmnEngineConfigurator.class);
            SpringContentEngineConfigurator contentConfigurator = context.getBean(SpringContentEngineConfigurator.class);
            SpringDmnEngineConfigurator dmnConfigurator = context.getBean(SpringDmnEngineConfigurator.class);
            SpringFormEngineConfigurator formConfigurator = context.getBean(SpringFormEngineConfigurator.class);
            SpringIdmEngineConfigurator idmConfigurator = context.getBean(SpringIdmEngineConfigurator.class);
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
            
            assertThat(appEngineConfiguration.getExpressionManager()).isInstanceOf(SpringAppExpressionManager.class);
            assertThat(appEngineConfiguration.getExpressionManager().getBeans()).isNull();
            assertThat(processEngineConfiguration.getExpressionManager()).isInstanceOf(SpringExpressionManager.class);
            assertThat(processEngineConfiguration.getExpressionManager().getBeans()).isInstanceOf(SpringBeanFactoryProxyMap.class);
            assertThat(cmmnEngineConfiguration.getExpressionManager()).isInstanceOf(SpringCmmnExpressionManager.class);
            assertThat(cmmnEngineConfiguration.getExpressionManager().getBeans()).isInstanceOf(SpringBeanFactoryProxyMap.class);
            assertThat(dmnEngineConfiguration.getExpressionManager()).isInstanceOf(SpringDmnExpressionManager.class);
            assertThat(dmnEngineConfiguration.getExpressionManager().getBeans()).isInstanceOf(SpringBeanFactoryProxyMap.class);
            assertThat(formEngineConfiguration.getExpressionManager()).isInstanceOf(SpringFormExpressionManager.class);
            assertThat(formEngineConfiguration.getExpressionManager().getBeans()).isInstanceOf(SpringBeanFactoryProxyMap.class);

            deleteDeployments(context.getBean(AppEngine.class));
            deleteDeployments(context.getBean(CmmnEngine.class));
            deleteDeployments(context.getBean(DmnEngine.class));
            deleteDeployments(context.getBean(FormEngine.class));
            deleteDeployments(context.getBean(ProcessEngine.class));
        });

    }
}
