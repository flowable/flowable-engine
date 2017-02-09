package org.flowable.engine.test.api.v6;

import java.sql.SQLException;
import java.util.List;

import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.ProcessEngineLifecycleListener;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.cfg.ProcessEngineConfigurator;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.test.FlowableRule;
import org.flowable.engine.test.impl.logger.ProcessExecutionLoggerConfigurator;
import org.h2.tools.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parent class for internal Flowable tests.
 * 
 * Boots up a process engine and caches it.
 * 
 * When using H2 and the default schema name, it will also boot the H2 webapp (reachable with browser on http://localhost:8082/)
 * 
 * @author Joram Barrez
 */
public class AbstractFlowable6Test {
  
  private static final Logger logger = LoggerFactory.getLogger(AbstractFlowable6Test.class);

  public static String H2_TEST_JDBC_URL = "jdbc:h2:mem:activiti;DB_CLOSE_DELAY=1000";

  @Rule
  public FlowableRule activitiRule = new FlowableRule();

  protected static ProcessEngine cachedProcessEngine;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected FormService formService;
  protected HistoryService historyService;
  protected ManagementService managementService;

  @Before
  public void initProcessEngine() {
    if (cachedProcessEngine == null) {
      cachedProcessEngine = activitiRule.getProcessEngine();

      // Boot up H2 webapp
      if (cachedProcessEngine instanceof ProcessEngineImpl) {
        if (((ProcessEngineImpl) cachedProcessEngine).getProcessEngineConfiguration().getJdbcUrl().equals(H2_TEST_JDBC_URL)) {
          initializeH2WebApp(cachedProcessEngine);
        }
      }
    }

    this.processEngineConfiguration = (ProcessEngineConfigurationImpl) cachedProcessEngine.getProcessEngineConfiguration();
    this.repositoryService = cachedProcessEngine.getRepositoryService();
    this.runtimeService = cachedProcessEngine.getRuntimeService();
    this.taskService = cachedProcessEngine.getTaskService();
    this.formService = cachedProcessEngine.getFormService();
    this.historyService = cachedProcessEngine.getHistoryService();
    this.managementService = cachedProcessEngine.getManagementService();
  }

  @After
  public void resetClock() {
    activitiRule.getProcessEngine().getProcessEngineConfiguration().getClock().reset();
  }

  @After
  public void logCommandInvokerDebugInfo() {

    ProcessExecutionLoggerConfigurator loggerConfigurator = null;
    List<ProcessEngineConfigurator> configurators = ((ProcessEngineImpl) cachedProcessEngine).getProcessEngineConfiguration().getConfigurators();
    if (configurators != null && configurators.size() > 0) {
      for (ProcessEngineConfigurator configurator : configurators) {
        if (configurator instanceof ProcessExecutionLoggerConfigurator) {
          loggerConfigurator = (ProcessExecutionLoggerConfigurator) configurator;
          break;
        }
      }

      if (loggerConfigurator != null) {
        loggerConfigurator.getProcessExecutionLogger().logDebugInfo(true);
      }
    }
  }

  protected void initializeH2WebApp(ProcessEngine processEngine) {
    try {
      final Server server = Server.createWebServer("-web");

      // Shutdown hook
      final ProcessEngineConfiguration processEngineConfiguration = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration();
      final ProcessEngineLifecycleListener originalLifecycleListener = processEngineConfiguration.getProcessEngineLifecycleListener();
      processEngineConfiguration.setProcessEngineLifecycleListener(new ProcessEngineLifecycleListener() {

        @Override
        public void onProcessEngineClosed(ProcessEngine processEngine) {
          server.stop();
          originalLifecycleListener.onProcessEngineClosed(processEngine);
        }

        @Override
        public void onProcessEngineBuilt(ProcessEngine processEngine) {
          originalLifecycleListener.onProcessEngineBuilt(processEngine);
        }

      });

      // Actually start the web server
      server.start();

    } catch (SQLException e) {
      logger.warn("Could not start H2 webapp", e);
    }
  }

}
