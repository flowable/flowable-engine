package org.flowable.rest.conf;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.flowable.content.api.ContentEngineConfigurationApi;
import org.flowable.content.api.ContentService;
import org.flowable.content.spring.SpringContentEngineConfiguration;
import org.flowable.content.spring.configurator.SpringContentEngineConfigurator;
import org.flowable.dmn.api.DmnEngineConfigurationApi;
import org.flowable.dmn.api.DmnHistoryService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.spring.SpringDmnEngineConfiguration;
import org.flowable.dmn.spring.configurator.SpringDmnEngineConfigurator;
import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.EngineServiceUtil;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.spring.configurator.SpringFormEngineConfigurator;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class FlowableEngineConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableEngineConfiguration.class);
    
    protected static final String PROP_FS_ROOT = "contentstorage.fs.rootFolder";
    protected static final String PROP_FS_CREATE_ROOT = "contentstorage.fs.createRoot";

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    protected Environment environment;

    @Bean(name = "processEngineFactoryBean")
    public ProcessEngineFactoryBean processEngineFactoryBean() {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(processEngineConfiguration());
        return factoryBean;
    }

    @Bean(name = "processEngine")
    public ProcessEngine processEngine() {
        // Safe to call the getObject() on the @Bean annotated processEngineFactoryBean(), will be
        // the fully initialized object instanced from the factory and will NOT be created more than once
        try {
            return processEngineFactoryBean().getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Bean(name = "dmnEngineConfiguration")
    public DmnEngineConfigurationApi dmnEngineConfiguration() {
        ProcessEngineConfiguration processEngineConfiguration = processEngine().getProcessEngineConfiguration();
        return EngineServiceUtil.getDmnEngineConfiguration(processEngineConfiguration);
    }
    
    @Bean(name = "formEngineConfiguration")
    public FormEngineConfigurationApi formEngineConfiguration() {
        ProcessEngineConfiguration processEngineConfiguration = processEngine().getProcessEngineConfiguration();
        return EngineServiceUtil.getFormEngineConfiguration(processEngineConfiguration);
    }
    
    @Bean(name = "contentEngineConfiguration")
    public ContentEngineConfigurationApi contentEngineConfiguration() {
        ProcessEngineConfiguration processEngineConfiguration = processEngine().getProcessEngineConfiguration();
        return EngineServiceUtil.getContentEngineConfiguration(processEngineConfiguration);
    }

    @Bean(name = "processEngineConfiguration")
    public ProcessEngineConfigurationImpl processEngineConfiguration() {
        SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
        processEngineConfiguration.setDataSource(dataSource);
        processEngineConfiguration.setDatabaseSchemaUpdate(environment.getProperty("engine.process.schema.update", "true"));
        processEngineConfiguration.setTransactionManager(transactionManager);
        processEngineConfiguration.setAsyncExecutorActivate(Boolean.valueOf(environment.getProperty("engine.process.asyncexecutor.activate", "true")));
        processEngineConfiguration.setHistory(environment.getProperty("engine.process.history.level", "full"));
        
        String emailHost = environment.getProperty("email.host");
        if (StringUtils.isNotEmpty(emailHost)) {
            processEngineConfiguration.setMailServerHost(emailHost);
            processEngineConfiguration.setMailServerPort(environment.getRequiredProperty("email.port", Integer.class));

            Boolean useCredentials = environment.getProperty("email.useCredentials", Boolean.class);
            if (Boolean.TRUE.equals(useCredentials)) {
                processEngineConfiguration.setMailServerUsername(environment.getProperty("email.username"));
                processEngineConfiguration.setMailServerPassword(environment.getProperty("email.password"));
            }
            
            Boolean useSSL = environment.getProperty("email.useSSL", Boolean.class);
            if (Boolean.TRUE.equals(useSSL)) {
                processEngineConfiguration.setMailServerUseSSL(true);
            }
            
            Boolean useTLS = environment.getProperty("email.useTLS", Boolean.class);
            if (Boolean.TRUE.equals(useTLS)) {
                processEngineConfiguration.setMailServerUseTLS(useTLS);
            }
        }

        // Limit process definition cache
        processEngineConfiguration.setProcessDefinitionCacheLimit(environment.getProperty("flowable.process-definitions.cache.max", Integer.class, 128));

        // Enable safe XML. See http://www.flowable.org/docs/userguide/index.html#advanced.safe.bpmn.xml
        processEngineConfiguration.setEnableSafeBpmnXml(true);

        processEngineConfiguration.addConfigurator(new SpringFormEngineConfigurator());
        
        SpringDmnEngineConfiguration dmnEngineConfiguration = new SpringDmnEngineConfiguration();
        dmnEngineConfiguration.setHistoryEnabled(true);
        SpringDmnEngineConfigurator dmnEngineConfigurator = new SpringDmnEngineConfigurator();
        dmnEngineConfigurator.setDmnEngineConfiguration(dmnEngineConfiguration);
        processEngineConfiguration.addConfigurator(dmnEngineConfigurator);
        
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
    public IdentityService identityService() {
        return processEngine().getIdentityService();
    }
    
    @Bean
    public IdmIdentityService idmIdentityService() {
        return ((IdmEngineConfiguration) processEngine().getProcessEngineConfiguration().getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG)).getIdmIdentityService();
    }

    @Bean
    public ManagementService managementService() {
        return processEngine().getManagementService();
    }

    @Bean
    public FormRepositoryService formRepositoryService() {
        return formEngineConfiguration().getFormRepositoryService();
    }

    @Bean
    public org.flowable.form.api.FormService formEngineFormService() {
        return formEngineConfiguration().getFormService();
    }

    @Bean
    public DmnRepositoryService dmnRepositoryService() {
        return dmnEngineConfiguration().getDmnRepositoryService();
    }

    @Bean
    public DmnRuleService dmnRuleService() {
        return dmnEngineConfiguration().getDmnRuleService();
    }
    
    @Bean
    public DmnHistoryService dmnHistoryService() {
        return dmnEngineConfiguration().getDmnHistoryService();
    }

    @Bean
    public ContentService contentService() {
        return contentEngineConfiguration().getContentService();
    }
}
