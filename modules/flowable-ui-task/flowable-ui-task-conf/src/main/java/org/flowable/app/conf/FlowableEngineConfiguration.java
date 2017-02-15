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
package org.flowable.app.conf;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.flowable.content.api.ContentService;
import org.flowable.content.spring.SpringContentEngineConfiguration;
import org.flowable.content.spring.configurator.SpringContentEngineConfigurator;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.spring.configurator.SpringDmnEngineConfigurator;
import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.common.runtime.Clock;
import org.flowable.engine.impl.asyncexecutor.AsyncExecutor;
import org.flowable.engine.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.spring.configurator.SpringFormEngineConfigurator;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@ComponentScan(basePackages = {
        "org.flowable.app.extension.conf", // For custom configuration classes
        "org.flowable.app.extension.bean" // For custom beans (delegates etc.)
})
public class FlowableEngineConfiguration {

    private final Logger logger = LoggerFactory.getLogger(FlowableEngineConfiguration.class);

    private static final String PROP_FS_ROOT = "contentstorage.fs.rootFolder";
    private static final String PROP_FS_CREATE_ROOT = "contentstorage.fs.createRoot";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Environment environment;

    @Bean(name = "processEngine")
    public ProcessEngineFactoryBean processEngineFactoryBean() {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(processEngineConfiguration());
        return factoryBean;
    }

    public ProcessEngine processEngine() {
        // Safe to call the getObject() on the @Bean annotated processEngineFactoryBean(), will be
        // the fully initialized object instanced from the factory and will NOT be created more than once
        try {
            return processEngineFactoryBean().getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean(name = "processEngineConfiguration")
    public ProcessEngineConfigurationImpl processEngineConfiguration() {
        SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
        processEngineConfiguration.setDataSource(dataSource);
        processEngineConfiguration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        processEngineConfiguration.setTransactionManager(transactionManager);
        processEngineConfiguration.setAsyncExecutorActivate(true);
        processEngineConfiguration.setAsyncExecutor(asyncExecutor());

        String emailHost = environment.getProperty("email.host");
        if (StringUtils.isNotEmpty(emailHost)) {
            processEngineConfiguration.setMailServerHost(emailHost);
            processEngineConfiguration.setMailServerPort(environment.getRequiredProperty("email.port", Integer.class));

            Boolean useCredentials = environment.getProperty("email.useCredentials", Boolean.class);
            if (Boolean.TRUE.equals(useCredentials)) {
                processEngineConfiguration.setMailServerUsername(environment.getProperty("email.username"));
                processEngineConfiguration.setMailServerPassword(environment.getProperty("email.password"));
            }
        }

        // Limit process definition cache
        processEngineConfiguration.setProcessDefinitionCacheLimit(environment.getProperty("flowable.process-definitions.cache.max", Integer.class, 128));

        // Enable safe XML. See http://www.flowable.org/docs/userguide/index.html#advanced.safe.bpmn.xml
        processEngineConfiguration.setEnableSafeBpmnXml(true);

        processEngineConfiguration.setDisableIdmEngine(true);
        processEngineConfiguration.addConfigurator(new SpringFormEngineConfigurator());
        processEngineConfiguration.addConfigurator(new SpringDmnEngineConfigurator());

        SpringContentEngineConfiguration contentEngineConfiguration = new SpringContentEngineConfiguration();
        String contentRootFolder = environment.getProperty(PROP_FS_ROOT);
        if (contentRootFolder != null) {
            contentEngineConfiguration.setContentRootFolder(contentRootFolder);
        }

        Boolean createRootFolder = environment.getProperty(PROP_FS_CREATE_ROOT, Boolean.class);
        if (createRootFolder != null) {
            contentEngineConfiguration.setCreateContentRootFolder(createRootFolder);
        }

        SpringContentEngineConfigurator springContentEngineConfigurator = new SpringContentEngineConfigurator();
        springContentEngineConfigurator.setContentEngineConfiguration(contentEngineConfiguration);

        processEngineConfiguration.addConfigurator(springContentEngineConfigurator);

        return processEngineConfiguration;
    }

    @Bean
    public AsyncExecutor asyncExecutor() {
        DefaultAsyncJobExecutor asyncExecutor = new DefaultAsyncJobExecutor();
        asyncExecutor.setDefaultAsyncJobAcquireWaitTimeInMillis(5000);
        asyncExecutor.setDefaultTimerJobAcquireWaitTimeInMillis(5000);
        return asyncExecutor;
    }

    @Bean(name = "clock")
    @DependsOn("processEngine")
    public Clock getClock() {
        return processEngineConfiguration().getClock();
    }

    @Bean
    public RepositoryService repositoryService() {
        return processEngine().getRepositoryService();
    }

    @Bean
    public RuntimeService runtimeService() {
        return processEngine().getRuntimeService();
    }

    @Bean
    public TaskService taskService() {
        return processEngine().getTaskService();
    }

    @Bean
    public HistoryService historyService() {
        return processEngine().getHistoryService();
    }

    @Bean
    public FormService formService() {
        return processEngine().getFormService();
    }

    @Bean
    public ManagementService managementService() {
        return processEngine().getManagementService();
    }

    @Bean
    public FormRepositoryService formEngineRepositoryService() {
        return processEngine().getFormEngineRepositoryService();
    }

    @Bean
    public org.flowable.form.api.FormService formEngineFormService() {
        return processEngine().getFormEngineFormService();
    }

    @Bean
    public DmnRepositoryService dmnRepositoryService() {
        return processEngine().getDmnRepositoryService();
    }

    @Bean
    public DmnRuleService dmnRuleService() {
        return processEngine().getDmnRuleService();
    }

    @Bean
    public ContentService contentService() {
        return processEngine().getContentService();
    }
}
