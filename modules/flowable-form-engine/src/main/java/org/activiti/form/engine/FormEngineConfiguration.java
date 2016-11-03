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
package org.activiti.form.engine;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.activiti.editor.form.converter.FormJsonConverter;
import org.activiti.engine.AbstractEngineConfiguration;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.cfg.TransactionContextFactory;
import org.activiti.engine.impl.interceptor.CommandConfig;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.runtime.Clock;
import org.activiti.form.api.FormRepositoryService;
import org.activiti.form.api.FormService;
import org.activiti.form.engine.impl.FormEngineImpl;
import org.activiti.form.engine.impl.FormRepositoryServiceImpl;
import org.activiti.form.engine.impl.FormServiceImpl;
import org.activiti.form.engine.impl.ServiceImpl;
import org.activiti.form.engine.impl.cfg.CommandExecutorImpl;
import org.activiti.form.engine.impl.cfg.StandaloneFormEngineConfiguration;
import org.activiti.form.engine.impl.cfg.StandaloneInMemFormEngineConfiguration;
import org.activiti.form.engine.impl.cfg.TransactionListener;
import org.activiti.form.engine.impl.cfg.standalone.StandaloneMybatisTransactionContextFactory;
import org.activiti.form.engine.impl.db.DbSqlSessionFactory;
import org.activiti.form.engine.impl.deployer.CachingAndArtifactsManager;
import org.activiti.form.engine.impl.deployer.FormDeployer;
import org.activiti.form.engine.impl.deployer.FormDeploymentHelper;
import org.activiti.form.engine.impl.deployer.ParsedDeploymentBuilderFactory;
import org.activiti.form.engine.impl.el.ExpressionManager;
import org.activiti.form.engine.impl.interceptor.CommandContext;
import org.activiti.form.engine.impl.interceptor.CommandContextFactory;
import org.activiti.form.engine.impl.interceptor.CommandContextInterceptor;
import org.activiti.form.engine.impl.interceptor.CommandExecutor;
import org.activiti.form.engine.impl.interceptor.CommandInterceptor;
import org.activiti.form.engine.impl.interceptor.CommandInvoker;
import org.activiti.form.engine.impl.interceptor.LogInterceptor;
import org.activiti.form.engine.impl.interceptor.TransactionContextInterceptor;
import org.activiti.form.engine.impl.parser.FormParseFactory;
import org.activiti.form.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.activiti.form.engine.impl.persistence.deploy.Deployer;
import org.activiti.form.engine.impl.persistence.deploy.DeploymentCache;
import org.activiti.form.engine.impl.persistence.deploy.DeploymentManager;
import org.activiti.form.engine.impl.persistence.deploy.FormCacheEntry;
import org.activiti.form.engine.impl.persistence.entity.FormDeploymentEntityManager;
import org.activiti.form.engine.impl.persistence.entity.FormDeploymentEntityManagerImpl;
import org.activiti.form.engine.impl.persistence.entity.FormEntityManager;
import org.activiti.form.engine.impl.persistence.entity.FormEntityManagerImpl;
import org.activiti.form.engine.impl.persistence.entity.ResourceEntityManager;
import org.activiti.form.engine.impl.persistence.entity.ResourceEntityManagerImpl;
import org.activiti.form.engine.impl.persistence.entity.SubmittedFormEntityManager;
import org.activiti.form.engine.impl.persistence.entity.SubmittedFormEntityManagerImpl;
import org.activiti.form.engine.impl.persistence.entity.data.FormDataManager;
import org.activiti.form.engine.impl.persistence.entity.data.FormDeploymentDataManager;
import org.activiti.form.engine.impl.persistence.entity.data.ResourceDataManager;
import org.activiti.form.engine.impl.persistence.entity.data.SubmittedFormDataManager;
import org.activiti.form.engine.impl.persistence.entity.data.impl.MybatisFormDataManager;
import org.activiti.form.engine.impl.persistence.entity.data.impl.MybatisFormDeploymentDataManager;
import org.activiti.form.engine.impl.persistence.entity.data.impl.MybatisResourceDataManager;
import org.activiti.form.engine.impl.persistence.entity.data.impl.MybatisSubmittedFormDataManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

public class FormEngineConfiguration extends AbstractEngineConfiguration {

  protected static final Logger logger = LoggerFactory.getLogger(FormEngineConfiguration.class);

  public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/activiti/form/db/mapping/mappings.xml";
  
  public static final String LIQUIBASE_CHANGELOG_PREFIX = "ACT_FO_";

  protected String formEngineName = FormEngines.NAME_DEFAULT;
  
  // COMMAND EXECUTORS ///////////////////////////////////////////////

  protected CommandInterceptor commandInvoker;

  /**
   * the configurable list which will be {@link #initInterceptorChain(java.util.List) processed} to build the {@link #commandExecutor}
   */
  protected List<CommandInterceptor> customPreCommandInterceptors;
  protected List<CommandInterceptor> customPostCommandInterceptors;

  protected List<CommandInterceptor> commandInterceptors;

  /** this will be initialized during the configurationComplete() */
  protected CommandExecutor commandExecutor;

  // SERVICES
  // /////////////////////////////////////////////////////////////////

  protected FormRepositoryService repositoryService = new FormRepositoryServiceImpl();
  protected FormService formService = new FormServiceImpl();

  // DATA MANAGERS ///////////////////////////////////////////////////

  protected FormDeploymentDataManager deploymentDataManager;
  protected FormDataManager formDataManager;
  protected ResourceDataManager resourceDataManager;
  protected SubmittedFormDataManager submittedFormDataManager;

  // ENTITY MANAGERS /////////////////////////////////////////////////
  protected FormDeploymentEntityManager deploymentEntityManager;
  protected FormEntityManager formEntityManager;
  protected ResourceEntityManager resourceEntityManager;
  protected SubmittedFormEntityManager submittedFormEntityManager;

  protected CommandContextFactory commandContextFactory;
  protected TransactionContextFactory<TransactionListener, CommandContext> transactionContextFactory;
  
  protected ExpressionManager expressionManager;
  
  protected FormJsonConverter formJsonConverter = new FormJsonConverter();

  // SESSION FACTORIES ///////////////////////////////////////////////
  protected DbSqlSessionFactory dbSqlSessionFactory;
  
  protected ObjectMapper objectMapper = new ObjectMapper();

  // DEPLOYERS
  // ////////////////////////////////////////////////////////////////

  protected FormDeployer formDeployer;
  protected FormParseFactory formParseFactory;
  protected ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory;
  protected FormDeploymentHelper formDeploymentHelper;
  protected CachingAndArtifactsManager cachingAndArtifactsManager;
  protected List<Deployer> customPreDeployers;
  protected List<Deployer> customPostDeployers;
  protected List<Deployer> deployers;
  protected DeploymentManager deploymentManager;

  protected int formCacheLimit = -1; // By default, no limit
  protected DeploymentCache<FormCacheEntry> formCache;

  public static FormEngineConfiguration createFormEngineConfigurationFromResourceDefault() {
    return createFormEngineConfigurationFromResource("activiti.form.cfg.xml", "formEngineConfiguration");
  }

  public static FormEngineConfiguration createFormEngineConfigurationFromResource(String resource) {
    return createFormEngineConfigurationFromResource(resource, "formEngineConfiguration");
  }

  public static FormEngineConfiguration createFormEngineConfigurationFromResource(String resource, String beanName) {
    return (FormEngineConfiguration) parseEngineConfigurationFromResource(resource, beanName);
  }

  public static FormEngineConfiguration createFormEngineConfigurationFromInputStream(InputStream inputStream) {
    return createFormEngineConfigurationFromInputStream(inputStream, "formEngineConfiguration");
  }

  public static FormEngineConfiguration createFormEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
    return (FormEngineConfiguration) parseEngineConfigurationFromInputStream(inputStream, beanName);
  }

  public static FormEngineConfiguration createStandaloneFormEngineConfiguration() {
    return new StandaloneFormEngineConfiguration();
  }

  public static FormEngineConfiguration createStandaloneInMemFormEngineConfiguration() {
    return new StandaloneInMemFormEngineConfiguration();
  }

  // buildProcessEngine
  // ///////////////////////////////////////////////////////

  public FormEngine buildFormEngine() {
    init();
    return new FormEngineImpl(this);
  }

  // init
  // /////////////////////////////////////////////////////////////////////

  protected void init() {
    initExpressionManager();
    initCommandContextFactory();
    initTransactionContextFactory();
    initCommandExecutors();
    initIdGenerator();
    
    if (usingRelationalDatabase) {
      initDataSource();
      initDbSchema();
    }
    
    initTransactionFactory();
    initSqlSessionFactory();
    initSessionFactories();
    initServices();
    initDataManagers();
    initEntityManagers();
    initDeployers();
    initClock();
  }

  // services
  // /////////////////////////////////////////////////////////////////

  protected void initServices() {
    initService(repositoryService);
    initService(formService);
  }

  protected void initService(Object service) {
    if (service instanceof ServiceImpl) {
      ((ServiceImpl) service).setCommandExecutor(commandExecutor);
    }
  }
  
  public void initExpressionManager() {
    if (expressionManager == null) {
      expressionManager = new ExpressionManager();
    }
  }

  // Data managers
  ///////////////////////////////////////////////////////////

  public void initDataManagers() {
    if (deploymentDataManager == null) {
      deploymentDataManager = new MybatisFormDeploymentDataManager(this);
    }
    if (formDataManager == null) {
      formDataManager = new MybatisFormDataManager(this);
    }
    if (resourceDataManager == null) {
      resourceDataManager = new MybatisResourceDataManager(this);
    }
    if (submittedFormDataManager == null) {
      submittedFormDataManager = new MybatisSubmittedFormDataManager(this);
    }
  }

  public void initEntityManagers() {
    if (deploymentEntityManager == null) {
      deploymentEntityManager = new FormDeploymentEntityManagerImpl(this, deploymentDataManager);
    }
    if (formEntityManager == null) {
      formEntityManager = new FormEntityManagerImpl(this, formDataManager);
    }
    if (resourceEntityManager == null) {
      resourceEntityManager = new ResourceEntityManagerImpl(this, resourceDataManager);
    }
    if (submittedFormEntityManager == null) {
      submittedFormEntityManager = new SubmittedFormEntityManagerImpl(this, submittedFormDataManager);
    }
  }
  
  // data model ///////////////////////////////////////////////////////////////

  public void initDbSchema() {
    try {
      DatabaseConnection connection = new JdbcConnection(dataSource.getConnection());
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
      database.setDatabaseChangeLogTableName(LIQUIBASE_CHANGELOG_PREFIX+database.getDatabaseChangeLogTableName());
      database.setDatabaseChangeLogLockTableName(LIQUIBASE_CHANGELOG_PREFIX+database.getDatabaseChangeLogLockTableName());
      
      if (StringUtils.isNotEmpty(databaseSchema)) {
        database.setDefaultSchemaName(databaseSchema);
        database.setLiquibaseSchemaName(databaseSchema);
      }
      
      if (StringUtils.isNotEmpty(databaseCatalog)) {
        database.setDefaultCatalogName(databaseCatalog);
        database.setLiquibaseCatalogName(databaseCatalog);
      }

      Liquibase liquibase = new Liquibase("org/activiti/form/db/liquibase/activiti-form-db-changelog.xml", new ClassLoaderResourceAccessor(), database);

      if (DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
        logger.debug("Dropping and creating schema FORM");
        liquibase.dropAll();
        liquibase.update("form");
      } else if (DB_SCHEMA_UPDATE_TRUE.equals(databaseSchemaUpdate)) {
        logger.debug("Updating schema FORM");
        liquibase.update("form");
      } else if (DB_SCHEMA_UPDATE_FALSE.equals(databaseSchemaUpdate)) {
        logger.debug("Validating schema FORM");
        liquibase.validate();
      }
    } catch (Exception e) {
      throw new ActivitiException("Error initialising form data schema", e);
    }
  }

  // session factories ////////////////////////////////////////////////////////

  public void initSessionFactories() {
    if (sessionFactories == null) {
      sessionFactories = new HashMap<Class<?>, SessionFactory>();

      if (usingRelationalDatabase) {
        initDbSqlSessionFactory();
      }
    }

    if (customSessionFactories != null) {
      for (SessionFactory sessionFactory : customSessionFactories) {
        addSessionFactory(sessionFactory);
      }
    }
  }

  public void initDbSqlSessionFactory() {
    if (dbSqlSessionFactory == null) {
      dbSqlSessionFactory = createDbSqlSessionFactory();
    }
    dbSqlSessionFactory.setDatabaseType(databaseType);
    dbSqlSessionFactory.setSqlSessionFactory(sqlSessionFactory);
    dbSqlSessionFactory.setIdGenerator(idGenerator);
    dbSqlSessionFactory.setDatabaseTablePrefix(databaseTablePrefix);
    dbSqlSessionFactory.setTablePrefixIsSchema(tablePrefixIsSchema);
    dbSqlSessionFactory.setDatabaseCatalog(databaseCatalog);
    dbSqlSessionFactory.setDatabaseSchema(databaseSchema);
    addSessionFactory(dbSqlSessionFactory);
  }

  public DbSqlSessionFactory createDbSqlSessionFactory() {
    return new DbSqlSessionFactory();
  }

  // command executors
  // ////////////////////////////////////////////////////////

  public void initCommandExecutors() {
    initDefaultCommandConfig();
    initSchemaCommandConfig();
    initCommandInvoker();
    initCommandInterceptors();
    initCommandExecutor();
  }

  public void initCommandInvoker() {
    if (commandInvoker == null) {
      commandInvoker = new CommandInvoker();
    }
  }

  public void initCommandInterceptors() {
    if (commandInterceptors == null) {
      commandInterceptors = new ArrayList<CommandInterceptor>();
      if (customPreCommandInterceptors != null) {
        commandInterceptors.addAll(customPreCommandInterceptors);
      }
      commandInterceptors.addAll(getDefaultCommandInterceptors());
      if (customPostCommandInterceptors != null) {
        commandInterceptors.addAll(customPostCommandInterceptors);
      }
      commandInterceptors.add(commandInvoker);
    }
  }

  public Collection<? extends CommandInterceptor> getDefaultCommandInterceptors() {
    List<CommandInterceptor> interceptors = new ArrayList<CommandInterceptor>();
    interceptors.add(new LogInterceptor());

    interceptors.add(new CommandContextInterceptor(commandContextFactory, this));
    
    CommandInterceptor transactionInterceptor = createTransactionInterceptor();
    if (transactionInterceptor != null) {
      interceptors.add(transactionInterceptor);
    }
    
    return interceptors;
  }

  public void initCommandExecutor() {
    if (commandExecutor == null) {
      CommandInterceptor first = initInterceptorChain(commandInterceptors);
      commandExecutor = new CommandExecutorImpl(getDefaultCommandConfig(), first);
    }
  }

  public CommandInterceptor initInterceptorChain(List<CommandInterceptor> chain) {
    if (chain == null || chain.isEmpty()) {
      throw new ActivitiException("invalid command interceptor chain configuration: " + chain);
    }
    for (int i = 0; i < chain.size() - 1; i++) {
      chain.get(i).setNext(chain.get(i + 1));
    }
    return chain.get(0);
  }

  public CommandInterceptor createTransactionInterceptor() {
    if (transactionContextFactory != null) {
      return new TransactionContextInterceptor(transactionContextFactory);
    } else {
      return null;
    }
  }

  // deployers
  // ////////////////////////////////////////////////////////////////

  protected void initDeployers() {
    if (formParseFactory == null) {
      formParseFactory = new FormParseFactory();
    }

    if (this.formDeployer == null) {
      this.deployers = new ArrayList<Deployer>();
      if (customPreDeployers != null) {
        this.deployers.addAll(customPreDeployers);
      }
      this.deployers.addAll(getDefaultDeployers());
      if (customPostDeployers != null) {
        this.deployers.addAll(customPostDeployers);
      }
    }

    // Decision cache
    if (formCache == null) {
      if (formCacheLimit <= 0) {
        formCache = new DefaultDeploymentCache<FormCacheEntry>();
      } else {
        formCache = new DefaultDeploymentCache<FormCacheEntry>(formCacheLimit);
      }
    }

    deploymentManager = new DeploymentManager(formCache, this);
    deploymentManager.setDeployers(deployers);
    deploymentManager.setDeploymentEntityManager(deploymentEntityManager);
    deploymentManager.setFormEntityManager(formEntityManager);
  }

  public Collection<? extends Deployer> getDefaultDeployers() {
    List<Deployer> defaultDeployers = new ArrayList<Deployer>();

    if (formDeployer == null) {
      formDeployer = new FormDeployer();
    }

    initDmnDeployerDependencies();

    formDeployer.setIdGenerator(idGenerator);
    formDeployer.setParsedDeploymentBuilderFactory(parsedDeploymentBuilderFactory);
    formDeployer.setFormDeploymentHelper(formDeploymentHelper);
    formDeployer.setCachingAndArtifactsManager(cachingAndArtifactsManager);

    defaultDeployers.add(formDeployer);
    return defaultDeployers;
  }

  public void initDmnDeployerDependencies() {
    if (parsedDeploymentBuilderFactory == null) {
      parsedDeploymentBuilderFactory = new ParsedDeploymentBuilderFactory();
    }
    if (parsedDeploymentBuilderFactory.getFormParseFactory() == null) {
      parsedDeploymentBuilderFactory.setFormParseFactory(formParseFactory);
    }

    if (formDeploymentHelper == null) {
      formDeploymentHelper = new FormDeploymentHelper();
    }

    if (cachingAndArtifactsManager == null) {
      cachingAndArtifactsManager = new CachingAndArtifactsManager();
    }
  }

  // OTHER
  // ////////////////////////////////////////////////////////////////////

  public void initCommandContextFactory() {
    if (commandContextFactory == null) {
      commandContextFactory = new CommandContextFactory();
    }
    commandContextFactory.setDmnEngineConfiguration(this);
  }

  public void initTransactionContextFactory() {
    if (transactionContextFactory == null) {
      transactionContextFactory = new StandaloneMybatisTransactionContextFactory();
    }
  }

  // myBatis SqlSessionFactory
  // ////////////////////////////////////////////////

  public String pathToEngineDbProperties() {
    return "org/activiti/form/db/properties/" + databaseType + ".properties";
  }

  public InputStream getMyBatisXmlConfigurationStream() {
    return getResourceAsStream(DEFAULT_MYBATIS_MAPPING_FILE);
  }

  // getters and setters
  // //////////////////////////////////////////////////////

  public String getEngineName() {
    return formEngineName;
  }

  public FormEngineConfiguration setEngineName(String formEngineName) {
    this.formEngineName = formEngineName;
    return this;
  }

  public FormEngineConfiguration setDatabaseType(String databaseType) {
    this.databaseType = databaseType;
    return this;
  }

  public FormEngineConfiguration setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
    return this;
  }

  public FormEngineConfiguration setJdbcDriver(String jdbcDriver) {
    this.jdbcDriver = jdbcDriver;
    return this;
  }

  public FormEngineConfiguration setJdbcUrl(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
    return this;
  }

  public FormEngineConfiguration setJdbcUsername(String jdbcUsername) {
    this.jdbcUsername = jdbcUsername;
    return this;
  }

  public FormEngineConfiguration setJdbcPassword(String jdbcPassword) {
    this.jdbcPassword = jdbcPassword;
    return this;
  }

 public FormEngineConfiguration setJdbcMaxActiveConnections(int jdbcMaxActiveConnections) {
    this.jdbcMaxActiveConnections = jdbcMaxActiveConnections;
    return this;
  }

  public FormEngineConfiguration setJdbcMaxIdleConnections(int jdbcMaxIdleConnections) {
    this.jdbcMaxIdleConnections = jdbcMaxIdleConnections;
    return this;
  }

  public FormEngineConfiguration setJdbcMaxCheckoutTime(int jdbcMaxCheckoutTime) {
    this.jdbcMaxCheckoutTime = jdbcMaxCheckoutTime;
    return this;
  }

  public FormEngineConfiguration setJdbcMaxWaitTime(int jdbcMaxWaitTime) {
    this.jdbcMaxWaitTime = jdbcMaxWaitTime;
    return this;
  }

  public FormEngineConfiguration setJdbcPingEnabled(boolean jdbcPingEnabled) {
    this.jdbcPingEnabled = jdbcPingEnabled;
    return this;
  }

  public FormEngineConfiguration setJdbcPingConnectionNotUsedFor(int jdbcPingConnectionNotUsedFor) {
    this.jdbcPingConnectionNotUsedFor = jdbcPingConnectionNotUsedFor;
    return this;
  }

  public FormEngineConfiguration setJdbcDefaultTransactionIsolationLevel(int jdbcDefaultTransactionIsolationLevel) {
    this.jdbcDefaultTransactionIsolationLevel = jdbcDefaultTransactionIsolationLevel;
    return this;
  }

  public FormEngineConfiguration setJdbcPingQuery(String jdbcPingQuery) {
    this.jdbcPingQuery = jdbcPingQuery;
    return this;
  }

  public FormEngineConfiguration setDataSourceJndiName(String dataSourceJndiName) {
    this.dataSourceJndiName = dataSourceJndiName;
    return this;
  }

  public FormEngineConfiguration setXmlEncoding(String xmlEncoding) {
    this.xmlEncoding = xmlEncoding;
    return this;
  }

  public FormEngineConfiguration setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    return this;
  }

  public FormEngineConfiguration setDefaultCommandConfig(CommandConfig defaultCommandConfig) {
    this.defaultCommandConfig = defaultCommandConfig;
    return this;
  }

  public CommandInterceptor getCommandInvoker() {
    return commandInvoker;
  }

  public FormEngineConfiguration setCommandInvoker(CommandInterceptor commandInvoker) {
    this.commandInvoker = commandInvoker;
    return this;
  }

  public List<CommandInterceptor> getCustomPreCommandInterceptors() {
    return customPreCommandInterceptors;
  }

  public FormEngineConfiguration setCustomPreCommandInterceptors(List<CommandInterceptor> customPreCommandInterceptors) {
    this.customPreCommandInterceptors = customPreCommandInterceptors;
    return this;
  }

  public List<CommandInterceptor> getCustomPostCommandInterceptors() {
    return customPostCommandInterceptors;
  }

  public FormEngineConfiguration setCustomPostCommandInterceptors(List<CommandInterceptor> customPostCommandInterceptors) {
    this.customPostCommandInterceptors = customPostCommandInterceptors;
    return this;
  }

  public List<CommandInterceptor> getCommandInterceptors() {
    return commandInterceptors;
  }

  public FormEngineConfiguration setCommandInterceptors(List<CommandInterceptor> commandInterceptors) {
    this.commandInterceptors = commandInterceptors;
    return this;
  }

  public CommandExecutor getCommandExecutor() {
    return commandExecutor;
  }

  public FormEngineConfiguration setCommandExecutor(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
    return this;
  }

  public FormRepositoryService getFormRepositoryService() {
    return repositoryService;
  }

  public FormService getFormService() {
    return formService;
  }

  public DeploymentManager getDeploymentManager() {
    return deploymentManager;
  }

  public FormEngineConfiguration getFormEngineConfiguration() {
    return this;
  }

  public FormDeployer getFormDeployer() {
    return formDeployer;
  }

  public FormEngineConfiguration setFormDeployer(FormDeployer formDeployer) {
    this.formDeployer = formDeployer;
    return this;
  }

  public FormParseFactory getFormParseFactory() {
    return formParseFactory;
  }

  public FormEngineConfiguration setFormParseFactory(FormParseFactory formParseFactory) {
    this.formParseFactory = formParseFactory;
    return this;
  }

  public int getFormCacheLimit() {
    return formCacheLimit;
  }

  public FormEngineConfiguration setFormCacheLimit(int formCacheLimit) {
    this.formCacheLimit = formCacheLimit;
    return this;
  }

  public DeploymentCache<FormCacheEntry> getFormCache() {
    return formCache;
  }

  public FormEngineConfiguration setFormCache(DeploymentCache<FormCacheEntry> formCache) {
    this.formCache = formCache;
    return this;
  }

  public FormDeploymentDataManager getDeploymentDataManager() {
    return deploymentDataManager;
  }

  public FormEngineConfiguration setDeploymentDataManager(FormDeploymentDataManager deploymentDataManager) {
    this.deploymentDataManager = deploymentDataManager;
    return this;
  }

  public FormDataManager getFormDataManager() {
    return formDataManager;
  }

  public FormEngineConfiguration setFormDataManager(FormDataManager formDataManager) {
    this.formDataManager = formDataManager;
    return this;
  }

  public ResourceDataManager getResourceDataManager() {
    return resourceDataManager;
  }

  public FormEngineConfiguration setResourceDataManager(ResourceDataManager resourceDataManager) {
    this.resourceDataManager = resourceDataManager;
    return this;
  }

  public SubmittedFormDataManager getSubmittedFormDataManager() {
    return submittedFormDataManager;
  }

  public FormEngineConfiguration setSubmittedFormDataManager(SubmittedFormDataManager submittedFormDataManager) {
    this.submittedFormDataManager = submittedFormDataManager;
    return this;
  }

  public FormDeploymentEntityManager getDeploymentEntityManager() {
    return deploymentEntityManager;
  }

  public FormEngineConfiguration setDeploymentEntityManager(FormDeploymentEntityManager deploymentEntityManager) {
    this.deploymentEntityManager = deploymentEntityManager;
    return this;
  }

  public FormEntityManager getFormEntityManager() {
    return formEntityManager;
  }

  public FormEngineConfiguration setFormEntityManager(FormEntityManager formEntityManager) {
    this.formEntityManager = formEntityManager;
    return this;
  }

  public ResourceEntityManager getResourceEntityManager() {
    return resourceEntityManager;
  }

  public FormEngineConfiguration setResourceEntityManager(ResourceEntityManager resourceEntityManager) {
    this.resourceEntityManager = resourceEntityManager;
    return this;
  }
  
  public SubmittedFormEntityManager getSubmittedFormEntityManager() {
    return submittedFormEntityManager;
  }

  public FormEngineConfiguration setSubmittedFormEntityManager(SubmittedFormEntityManager submittedFormEntityManager) {
    this.submittedFormEntityManager = submittedFormEntityManager;
    return this;
  }

  public CommandContextFactory getCommandContextFactory() {
    return commandContextFactory;
  }

  public FormEngineConfiguration setCommandContextFactory(CommandContextFactory commandContextFactory) {
    this.commandContextFactory = commandContextFactory;
    return this;
  }

  public FormEngineConfiguration setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    return this;
  }

  public FormEngineConfiguration setTransactionFactory(TransactionFactory transactionFactory) {
    this.transactionFactory = transactionFactory;
    return this;
  }
  
  public ExpressionManager getExpressionManager() {
    return expressionManager;
  }

  public FormEngineConfiguration setExpressionManager(ExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
    return this;
  }

  public FormJsonConverter getFormJsonConverter() {
    return formJsonConverter;
  }

  public FormEngineConfiguration setFormJsonConverter(FormJsonConverter formJsonConverter) {
    this.formJsonConverter = formJsonConverter;
    return this;
  }

  public FormEngineConfiguration setCustomMybatisMappers(Set<Class<?>> customMybatisMappers) {
    this.customMybatisMappers = customMybatisMappers;
    return this;
  }

  public FormEngineConfiguration setCustomMybatisXMLMappers(Set<String> customMybatisXMLMappers) {
    this.customMybatisXMLMappers = customMybatisXMLMappers;
    return this;
  }

  public FormEngineConfiguration setCustomSessionFactories(List<SessionFactory> customSessionFactories) {
    this.customSessionFactories = customSessionFactories;
    return this;
  }

  public DbSqlSessionFactory getDbSqlSessionFactory() {
    return dbSqlSessionFactory;
  }

  public FormEngineConfiguration setDbSqlSessionFactory(DbSqlSessionFactory dbSqlSessionFactory) {
    this.dbSqlSessionFactory = dbSqlSessionFactory;
    return this;
  }

  public FormEngineConfiguration setUsingRelationalDatabase(boolean usingRelationalDatabase) {
    this.usingRelationalDatabase = usingRelationalDatabase;
    return this;
  }

  public FormEngineConfiguration setDatabaseTablePrefix(String databaseTablePrefix) {
    this.databaseTablePrefix = databaseTablePrefix;
    return this;
  }

  public FormEngineConfiguration setDatabaseCatalog(String databaseCatalog) {
    this.databaseCatalog = databaseCatalog;
    return this;
  }

  public FormEngineConfiguration setDatabaseSchema(String databaseSchema) {
    this.databaseSchema = databaseSchema;
    return this;
  }

  public FormEngineConfiguration setTablePrefixIsSchema(boolean tablePrefixIsSchema) {
    this.tablePrefixIsSchema = tablePrefixIsSchema;
    return this;
  }

  public FormEngineConfiguration setSessionFactories(Map<Class<?>, SessionFactory> sessionFactories) {
    this.sessionFactories = sessionFactories;
    return this;
  }

  public TransactionContextFactory<TransactionListener, CommandContext> getTransactionContextFactory() {
    return transactionContextFactory;
  }

  public FormEngineConfiguration setTransactionContextFactory(
      TransactionContextFactory<TransactionListener, CommandContext> transactionContextFactory) {
    
    this.transactionContextFactory = transactionContextFactory;
    return this;
  }
  
  public FormEngineConfiguration setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
    this.databaseSchemaUpdate = databaseSchemaUpdate;
    return this;
  }

  public FormEngineConfiguration setClock(Clock clock) {
    this.clock = clock;
    return this;
  }
  
  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }
  
  public FormEngineConfiguration setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    return this;
  }
}
