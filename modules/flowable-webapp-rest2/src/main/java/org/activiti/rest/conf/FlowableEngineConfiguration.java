package org.activiti.rest.conf;

import java.util.ArrayList;
import java.util.List;

import org.activiti.dmn.engine.DmnEngineConfiguration;
import org.activiti.dmn.engine.configurator.DmnEngineConfigurator;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.AbstractFormType;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.form.engine.FormEngine;
import org.activiti.form.engine.FormEngineConfiguration;
import org.activiti.form.engine.configurator.FormEngineConfigurator;
import org.activiti.rest.form.MonthFormType;
import org.activiti.rest.form.ProcessDefinitionFormType;
import org.activiti.rest.form.UserFormType;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class FlowableEngineConfiguration extends BaseEngineConfiguration {

  private final Logger log = LoggerFactory.getLogger(FlowableEngineConfiguration.class);

  @Autowired
  protected DmnEngineConfiguration dmnEngineConfiguration;

  @Autowired
  protected FormEngineConfiguration formEngineConfiguration;

  @Bean(name = "processEngineFactoryBean")
  public ProcessEngineFactoryBean processEngineFactoryBean() {
    ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
    factoryBean.setProcessEngineConfiguration(processEngineConfiguration());
    return factoryBean;
  }

  @Bean(name = "processEngine")
  public ProcessEngine processEngine() {
    // Safe to call the getObject() on the @Bean annotated
    // processEngineFactoryBean(), will be
    // the fully initialized object instanced from the factory and will NOT
    // be created more than once
    try {
      return processEngineFactoryBean().getObject();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  @Bean(name = "processEngineConfiguration")
  public ProcessEngineConfigurationImpl processEngineConfiguration() {
    SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
    processEngineConfiguration.setDataSource(dataSource());
    processEngineConfiguration.setDatabaseSchemaUpdate(environment.getProperty("engine.process.schema.update", "true"));
    processEngineConfiguration.setTransactionManager(annotationDrivenTransactionManager());
    processEngineConfiguration.setAsyncExecutorActivate(Boolean.valueOf(environment.getProperty("engine.process.asyncexecutor.activate", "true")));
    processEngineConfiguration.setHistory(environment.getProperty("engine.process.history.level", "full"));

    List<AbstractFormType> formTypes = new ArrayList<AbstractFormType>();
    formTypes.add(new UserFormType());
    formTypes.add(new ProcessDefinitionFormType());
    formTypes.add(new MonthFormType());
    processEngineConfiguration.setCustomFormTypes(formTypes);

    FormEngineConfigurator formEngineConfigurator = new FormEngineConfigurator();
    formEngineConfigurator.setFormEngineConfiguration(formEngineConfiguration);
    processEngineConfiguration.addConfigurator(formEngineConfigurator);

    DmnEngineConfigurator dmnEngineConfigurator = new DmnEngineConfigurator();
    dmnEngineConfigurator.setDmnEngineConfiguration(dmnEngineConfiguration);
    processEngineConfiguration.addConfigurator(dmnEngineConfigurator);

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
  
}
