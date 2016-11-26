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
package org.activiti.content.engine;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.activiti.content.api.ContentManagementService;
import org.activiti.content.api.ContentService;
import org.activiti.content.api.ContentStorage;
import org.activiti.content.engine.impl.ContentEngineImpl;
import org.activiti.content.engine.impl.ContentManagementServiceImpl;
import org.activiti.content.engine.impl.ContentServiceImpl;
import org.activiti.content.engine.impl.ServiceImpl;
import org.activiti.content.engine.impl.cfg.CommandExecutorImpl;
import org.activiti.content.engine.impl.cfg.StandaloneContentEngineConfiguration;
import org.activiti.content.engine.impl.cfg.StandaloneInMemContentEngineConfiguration;
import org.activiti.content.engine.impl.cfg.TransactionListener;
import org.activiti.content.engine.impl.cfg.standalone.StandaloneMybatisTransactionContextFactory;
import org.activiti.content.engine.impl.db.DbSqlSessionFactory;
import org.activiti.content.engine.impl.fs.SimpleFileSystemContentStorage;
import org.activiti.content.engine.impl.interceptor.CommandContext;
import org.activiti.content.engine.impl.interceptor.CommandContextFactory;
import org.activiti.content.engine.impl.interceptor.CommandContextInterceptor;
import org.activiti.content.engine.impl.interceptor.CommandExecutor;
import org.activiti.content.engine.impl.interceptor.CommandInterceptor;
import org.activiti.content.engine.impl.interceptor.CommandInvoker;
import org.activiti.content.engine.impl.interceptor.LogInterceptor;
import org.activiti.content.engine.impl.interceptor.TransactionContextInterceptor;
import org.activiti.content.engine.impl.persistence.entity.ContentItemEntityManager;
import org.activiti.content.engine.impl.persistence.entity.ContentItemEntityManagerImpl;
import org.activiti.content.engine.impl.persistence.entity.TableDataManager;
import org.activiti.content.engine.impl.persistence.entity.TableDataManagerImpl;
import org.activiti.content.engine.impl.persistence.entity.data.ContentItemDataManager;
import org.activiti.content.engine.impl.persistence.entity.data.impl.MybatisContentItemDataManager;
import org.activiti.engine.common.AbstractEngineConfiguration;
import org.activiti.engine.common.api.ActivitiException;
import org.activiti.engine.common.impl.cfg.BeansConfigurationHelper;
import org.activiti.engine.common.impl.cfg.TransactionContextFactory;
import org.activiti.engine.common.impl.interceptor.CommandConfig;
import org.activiti.engine.common.impl.interceptor.SessionFactory;
import org.activiti.engine.common.runtime.Clock;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

public class ContentEngineConfiguration extends AbstractEngineConfiguration {

  protected static final Logger logger = LoggerFactory.getLogger(ContentEngineConfiguration.class);

  public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/activiti/content/db/mapping/mappings.xml";
  
  public static final String LIQUIBASE_CHANGELOG_PREFIX = "ACT_CO_";

  protected String contentEngineName = ContentEngines.NAME_DEFAULT;
  
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

  protected ContentManagementService contentManagementService = new ContentManagementServiceImpl();
  protected ContentService contentService = new ContentServiceImpl();

  // DATA MANAGERS ///////////////////////////////////////////////////

  protected ContentItemDataManager contentItemDataManager;
  
  // ADDITIONAL SERVICES /////////////////////////////////////////////
  
  protected ContentStorage contentStorage;
  protected String contentRootFolder;
  protected boolean createContentRootFolder = true;

  // ENTITY MANAGERS /////////////////////////////////////////////////
  protected ContentItemEntityManager contentItemEntityManager;
  protected TableDataManager tableDataManager;

  protected CommandContextFactory commandContextFactory;
  protected TransactionContextFactory<TransactionListener, CommandContext> transactionContextFactory;
  
  // SESSION FACTORIES ///////////////////////////////////////////////
  protected DbSqlSessionFactory dbSqlSessionFactory;

  public static ContentEngineConfiguration createContentEngineConfigurationFromResourceDefault() {
    return createContentEngineConfigurationFromResource("activiti.content.cfg.xml", "contentEngineConfiguration");
  }

  public static ContentEngineConfiguration createContentEngineConfigurationFromResource(String resource) {
    return createContentEngineConfigurationFromResource(resource, "contentEngineConfiguration");
  }

  public static ContentEngineConfiguration createContentEngineConfigurationFromResource(String resource, String beanName) {
    return (ContentEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromResource(resource, beanName);
  }

  public static ContentEngineConfiguration createContentEngineConfigurationFromInputStream(InputStream inputStream) {
    return createContentEngineConfigurationFromInputStream(inputStream, "contentEngineConfiguration");
  }

  public static ContentEngineConfiguration createContentEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
    return (ContentEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromInputStream(inputStream, beanName);
  }

  public static ContentEngineConfiguration createStandaloneContentEngineConfiguration() {
    return new StandaloneContentEngineConfiguration();
  }

  public static ContentEngineConfiguration createStandaloneInMemContentEngineConfiguration() {
    return new StandaloneInMemContentEngineConfiguration();
  }

  // buildProcessEngine
  // ///////////////////////////////////////////////////////

  public ContentEngine buildContentEngine() {
    init();
    return new ContentEngineImpl(this);
  }

  // init
  // /////////////////////////////////////////////////////////////////////

  protected void init() {
    initCommandContextFactory();
    initTransactionContextFactory();
    initCommandExecutors();
    initIdGenerator();
    
    if (usingRelationalDatabase) {
      initDataSource();
      initDbSchema();
    }
    
    initBeans();
    initTransactionFactory();
    initSqlSessionFactory();
    initSessionFactories();
    initServices();
    initDataManagers();
    initEntityManagers();
    initContentStorage();
    initClock();
  }

  // services
  // /////////////////////////////////////////////////////////////////

  protected void initServices() {
    initService(contentManagementService);
    initService(contentService);
  }

  protected void initService(Object service) {
    if (service instanceof ServiceImpl) {
      ((ServiceImpl) service).setCommandExecutor(commandExecutor);
    }
  }
  
  // Data managers
  ///////////////////////////////////////////////////////////

  public void initDataManagers() {
    if (contentItemDataManager == null) {
      contentItemDataManager = new MybatisContentItemDataManager(this);
    }
  }

  public void initEntityManagers() {
    if (contentItemEntityManager == null) {
      contentItemEntityManager = new ContentItemEntityManagerImpl(this, contentItemDataManager);
    }
    if (tableDataManager == null) {
      tableDataManager = new TableDataManagerImpl(this);
    }
  }
  
  public void initContentStorage() {
    if (contentStorage == null) {
      if (contentRootFolder == null) {
        contentRootFolder = System.getProperty("user.home") + File.separator + "content";
      }
      
      File contentRootFile = new File(contentRootFolder);
      if (createContentRootFolder && !contentRootFile.exists()) {
        contentRootFile.mkdirs();
      }
      
      if (contentRootFile != null && contentRootFile.exists()) {
        logger.info("Content file system root : " + contentRootFile.getAbsolutePath());
     }
      
      contentStorage = new SimpleFileSystemContentStorage(contentRootFile);
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

      Liquibase liquibase = new Liquibase("org/activiti/content/db/liquibase/activiti-content-db-changelog.xml", new ClassLoaderResourceAccessor(), database);

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
      throw new ActivitiException("Error initialising content data schema", e);
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
    return "org/activiti/content/db/properties/" + databaseType + ".properties";
  }

  public InputStream getMyBatisXmlConfigurationStream() {
    return getResourceAsStream(DEFAULT_MYBATIS_MAPPING_FILE);
  }

  // getters and setters
  // //////////////////////////////////////////////////////

  public String getEngineName() {
    return contentEngineName;
  }

  public ContentEngineConfiguration setEngineName(String contentEngineName) {
    this.contentEngineName = contentEngineName;
    return this;
  }

  public ContentEngineConfiguration setDatabaseType(String databaseType) {
    this.databaseType = databaseType;
    return this;
  }

  public ContentEngineConfiguration setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
    return this;
  }

  public ContentEngineConfiguration setJdbcDriver(String jdbcDriver) {
    this.jdbcDriver = jdbcDriver;
    return this;
  }

  public ContentEngineConfiguration setJdbcUrl(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
    return this;
  }

  public ContentEngineConfiguration setJdbcUsername(String jdbcUsername) {
    this.jdbcUsername = jdbcUsername;
    return this;
  }

  public ContentEngineConfiguration setJdbcPassword(String jdbcPassword) {
    this.jdbcPassword = jdbcPassword;
    return this;
  }

 public ContentEngineConfiguration setJdbcMaxActiveConnections(int jdbcMaxActiveConnections) {
    this.jdbcMaxActiveConnections = jdbcMaxActiveConnections;
    return this;
  }

  public ContentEngineConfiguration setJdbcMaxIdleConnections(int jdbcMaxIdleConnections) {
    this.jdbcMaxIdleConnections = jdbcMaxIdleConnections;
    return this;
  }

  public ContentEngineConfiguration setJdbcMaxCheckoutTime(int jdbcMaxCheckoutTime) {
    this.jdbcMaxCheckoutTime = jdbcMaxCheckoutTime;
    return this;
  }

  public ContentEngineConfiguration setJdbcMaxWaitTime(int jdbcMaxWaitTime) {
    this.jdbcMaxWaitTime = jdbcMaxWaitTime;
    return this;
  }

  public ContentEngineConfiguration setJdbcPingEnabled(boolean jdbcPingEnabled) {
    this.jdbcPingEnabled = jdbcPingEnabled;
    return this;
  }

  public ContentEngineConfiguration setJdbcPingConnectionNotUsedFor(int jdbcPingConnectionNotUsedFor) {
    this.jdbcPingConnectionNotUsedFor = jdbcPingConnectionNotUsedFor;
    return this;
  }

  public ContentEngineConfiguration setJdbcDefaultTransactionIsolationLevel(int jdbcDefaultTransactionIsolationLevel) {
    this.jdbcDefaultTransactionIsolationLevel = jdbcDefaultTransactionIsolationLevel;
    return this;
  }

  public ContentEngineConfiguration setJdbcPingQuery(String jdbcPingQuery) {
    this.jdbcPingQuery = jdbcPingQuery;
    return this;
  }

  public ContentEngineConfiguration setDataSourceJndiName(String dataSourceJndiName) {
    this.dataSourceJndiName = dataSourceJndiName;
    return this;
  }

  public ContentEngineConfiguration setXmlEncoding(String xmlEncoding) {
    this.xmlEncoding = xmlEncoding;
    return this;
  }

  public ContentEngineConfiguration setDefaultCommandConfig(CommandConfig defaultCommandConfig) {
    this.defaultCommandConfig = defaultCommandConfig;
    return this;
  }

  public CommandInterceptor getCommandInvoker() {
    return commandInvoker;
  }

  public ContentEngineConfiguration setCommandInvoker(CommandInterceptor commandInvoker) {
    this.commandInvoker = commandInvoker;
    return this;
  }

  public List<CommandInterceptor> getCustomPreCommandInterceptors() {
    return customPreCommandInterceptors;
  }

  public ContentEngineConfiguration setCustomPreCommandInterceptors(List<CommandInterceptor> customPreCommandInterceptors) {
    this.customPreCommandInterceptors = customPreCommandInterceptors;
    return this;
  }

  public List<CommandInterceptor> getCustomPostCommandInterceptors() {
    return customPostCommandInterceptors;
  }

  public ContentEngineConfiguration setCustomPostCommandInterceptors(List<CommandInterceptor> customPostCommandInterceptors) {
    this.customPostCommandInterceptors = customPostCommandInterceptors;
    return this;
  }

  public List<CommandInterceptor> getCommandInterceptors() {
    return commandInterceptors;
  }

  public ContentEngineConfiguration setCommandInterceptors(List<CommandInterceptor> commandInterceptors) {
    this.commandInterceptors = commandInterceptors;
    return this;
  }

  public CommandExecutor getCommandExecutor() {
    return commandExecutor;
  }

  public ContentEngineConfiguration setCommandExecutor(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
    return this;
  }
  
  public ContentManagementService getContentManagementService() {
    return contentManagementService;
  }
  
  public ContentEngineConfiguration setContentManagementService(ContentManagementService contentManagementService) {
    this.contentManagementService = contentManagementService;
    return this;
  }

  public ContentService getContentService() {
    return contentService;
  }
  
  public ContentEngineConfiguration setContentService(ContentService contentService) {
    this.contentService = contentService;
    return this;
  }

  public ContentEngineConfiguration getContentEngineConfiguration() {
    return this;
  }

  public ContentItemDataManager getContentItemDataManager() {
    return contentItemDataManager;
  }

  public ContentEngineConfiguration setContentItemDataManager(ContentItemDataManager contentItemDataManager) {
    this.contentItemDataManager = contentItemDataManager;
    return this;
  }

  public ContentItemEntityManager getContentItemEntityManager() {
    return contentItemEntityManager;
  }

  public ContentEngineConfiguration setContentItemEntityManager(ContentItemEntityManager contentItemEntityManager) {
    this.contentItemEntityManager = contentItemEntityManager;
    return this;
  }
  
  public TableDataManager getTableDataManager() {
    return tableDataManager;
  }

  public ContentEngineConfiguration setTableDataManager(TableDataManager tableDataManager) {
    this.tableDataManager = tableDataManager;
    return this;
  }

  public ContentStorage getContentStorage() {
    return contentStorage;
  }

  public ContentEngineConfiguration setContentStorage(ContentStorage contentStorage) {
    this.contentStorage = contentStorage;
    return this;
  }

  public String getContentRootFolder() {
    return contentRootFolder;
  }

  public ContentEngineConfiguration setContentRootFolder(String contentRootFolder) {
    this.contentRootFolder = contentRootFolder;
    return this;
  }

  public boolean isCreateContentRootFolder() {
    return createContentRootFolder;
  }

  public ContentEngineConfiguration setCreateContentRootFolder(boolean createContentRootFolder) {
    this.createContentRootFolder = createContentRootFolder;
    return this;
  }

  public CommandContextFactory getCommandContextFactory() {
    return commandContextFactory;
  }

  public ContentEngineConfiguration setCommandContextFactory(CommandContextFactory commandContextFactory) {
    this.commandContextFactory = commandContextFactory;
    return this;
  }

  public ContentEngineConfiguration setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    return this;
  }

  public ContentEngineConfiguration setTransactionFactory(TransactionFactory transactionFactory) {
    this.transactionFactory = transactionFactory;
    return this;
  }

  public ContentEngineConfiguration setCustomMybatisMappers(Set<Class<?>> customMybatisMappers) {
    this.customMybatisMappers = customMybatisMappers;
    return this;
  }

  public ContentEngineConfiguration setCustomMybatisXMLMappers(Set<String> customMybatisXMLMappers) {
    this.customMybatisXMLMappers = customMybatisXMLMappers;
    return this;
  }

  public ContentEngineConfiguration setCustomSessionFactories(List<SessionFactory> customSessionFactories) {
    this.customSessionFactories = customSessionFactories;
    return this;
  }

  public DbSqlSessionFactory getDbSqlSessionFactory() {
    return dbSqlSessionFactory;
  }

  public ContentEngineConfiguration setDbSqlSessionFactory(DbSqlSessionFactory dbSqlSessionFactory) {
    this.dbSqlSessionFactory = dbSqlSessionFactory;
    return this;
  }

  public ContentEngineConfiguration setUsingRelationalDatabase(boolean usingRelationalDatabase) {
    this.usingRelationalDatabase = usingRelationalDatabase;
    return this;
  }

  public ContentEngineConfiguration setDatabaseTablePrefix(String databaseTablePrefix) {
    this.databaseTablePrefix = databaseTablePrefix;
    return this;
  }

  public ContentEngineConfiguration setDatabaseCatalog(String databaseCatalog) {
    this.databaseCatalog = databaseCatalog;
    return this;
  }

  public ContentEngineConfiguration setDatabaseSchema(String databaseSchema) {
    this.databaseSchema = databaseSchema;
    return this;
  }

  public ContentEngineConfiguration setTablePrefixIsSchema(boolean tablePrefixIsSchema) {
    this.tablePrefixIsSchema = tablePrefixIsSchema;
    return this;
  }

  public ContentEngineConfiguration setSessionFactories(Map<Class<?>, SessionFactory> sessionFactories) {
    this.sessionFactories = sessionFactories;
    return this;
  }

  public TransactionContextFactory<TransactionListener, CommandContext> getTransactionContextFactory() {
    return transactionContextFactory;
  }

  public ContentEngineConfiguration setTransactionContextFactory(
      TransactionContextFactory<TransactionListener, CommandContext> transactionContextFactory) {
    
    this.transactionContextFactory = transactionContextFactory;
    return this;
  }
  
  public ContentEngineConfiguration setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
    this.databaseSchemaUpdate = databaseSchemaUpdate;
    return this;
  }

  public ContentEngineConfiguration setClock(Clock clock) {
    this.clock = clock;
    return this;
  }
}
