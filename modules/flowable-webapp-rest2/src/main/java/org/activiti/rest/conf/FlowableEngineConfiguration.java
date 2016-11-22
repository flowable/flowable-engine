package org.activiti.rest.conf;

import javax.sql.DataSource;

import org.activiti.dmn.api.DmnRepositoryService;
import org.activiti.dmn.api.DmnRuleService;
import org.activiti.dmn.spring.configurator.SpringDmnEngineConfigurator;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.form.api.FormRepositoryService;
import org.activiti.form.spring.configurator.SpringFormEngineConfigurator;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.activiti.spring.SpringProcessEngineConfiguration;
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
  public org.activiti.form.api.FormService formEngineFormService() {
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
}
