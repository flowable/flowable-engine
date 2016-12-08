package org.flowable.rest.conf;

import javax.sql.DataSource;

import org.flowable.content.api.ContentService;
import org.flowable.content.spring.configurator.SpringContentEngineConfigurator;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.spring.configurator.SpringDmnEngineConfigurator;
import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.spring.configurator.SpringFormEngineConfigurator;
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

  private final Logger log = LoggerFactory.getLogger(FlowableEngineConfiguration.class);
  
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
  
  @Bean(name = "processEngineConfiguration")
  public ProcessEngineConfigurationImpl processEngineConfiguration() {
    SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
    processEngineConfiguration.setDataSource(dataSource);
    processEngineConfiguration.setDatabaseSchemaUpdate(environment.getProperty("engine.process.schema.update", "true"));
    processEngineConfiguration.setTransactionManager(transactionManager);
    processEngineConfiguration.setAsyncExecutorActivate(Boolean.valueOf(environment.getProperty("engine.process.asyncexecutor.activate", "true")));
    processEngineConfiguration.setHistory(environment.getProperty("engine.process.history.level", "full"));

    processEngineConfiguration.addConfigurator(new SpringFormEngineConfigurator());
    processEngineConfiguration.addConfigurator(new SpringDmnEngineConfigurator());
    processEngineConfiguration.addConfigurator(new SpringContentEngineConfigurator());

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
  public ManagementService managementService() {
    return processEngine().getManagementService();
  }

  @Bean
  public FormRepositoryService formRepositoryService() {
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
