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
package org.activiti.idm.engine;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.activiti.idm.api.IdmIdentityService;
import org.activiti.idm.api.IdmManagementService;
import org.activiti.idm.api.event.ActivitiIdmEventDispatcher;
import org.activiti.idm.api.event.ActivitiIdmEventListener;
import org.activiti.idm.api.event.ActivitiIdmEventType;
import org.activiti.idm.engine.delegate.event.impl.ActivitiIdmEventDispatcherImpl;
import org.activiti.idm.engine.impl.IdmEngineImpl;
import org.activiti.idm.engine.impl.IdmIdentityServiceImpl;
import org.activiti.idm.engine.impl.IdmManagementServiceImpl;
import org.activiti.idm.engine.impl.ServiceImpl;
import org.activiti.idm.engine.impl.cfg.CommandExecutorImpl;
import org.activiti.idm.engine.impl.cfg.IdGenerator;
import org.activiti.idm.engine.impl.cfg.StandaloneIdmEngineConfiguration;
import org.activiti.idm.engine.impl.cfg.StandaloneInMemIdmEngineConfiguration;
import org.activiti.idm.engine.impl.cfg.TransactionContextFactory;
import org.activiti.idm.engine.impl.cfg.standalone.StandaloneMybatisTransactionContextFactory;
import org.activiti.idm.engine.impl.db.DbSqlSessionFactory;
import org.activiti.idm.engine.impl.interceptor.CommandConfig;
import org.activiti.idm.engine.impl.interceptor.CommandContextFactory;
import org.activiti.idm.engine.impl.interceptor.CommandContextInterceptor;
import org.activiti.idm.engine.impl.interceptor.CommandExecutor;
import org.activiti.idm.engine.impl.interceptor.CommandInterceptor;
import org.activiti.idm.engine.impl.interceptor.CommandInvoker;
import org.activiti.idm.engine.impl.interceptor.LogInterceptor;
import org.activiti.idm.engine.impl.interceptor.SessionFactory;
import org.activiti.idm.engine.impl.persistence.StrongUuidGenerator;
import org.activiti.idm.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.ByteArrayEntityManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.GroupEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.GroupEntityManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.IdentityInfoEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.IdentityInfoEntityManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.MembershipEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.MembershipEntityManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.PropertyEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.PropertyEntityManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.TableDataManager;
import org.activiti.idm.engine.impl.persistence.entity.TableDataManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.TokenEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.TokenEntityManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.UserEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.UserEntityManagerImpl;
import org.activiti.idm.engine.impl.persistence.entity.data.ByteArrayDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.GroupDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.IdentityInfoDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.MembershipDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.PropertyDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.TokenDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.UserDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.impl.MybatisByteArrayDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.impl.MybatisGroupDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.impl.MybatisIdentityInfoDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.impl.MybatisMembershipDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.impl.MybatisPropertyDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.impl.MybatisTokenDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.impl.MybatisUserDataManager;
import org.activiti.idm.engine.impl.util.DefaultClockImpl;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

public class IdmEngineConfiguration {

  protected static final Logger logger = LoggerFactory.getLogger(IdmEngineConfiguration.class);

  /** The tenant id indicating 'no tenant' */
  public static final String NO_TENANT_ID = "";

  public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/activiti/idm/db/mapping/mappings.xml";
  
  /**
   * Checks the version of the DB schema against the library when the form engine is being created and throws an exception if the versions don't match.
   */
  public static final String DB_SCHEMA_UPDATE_FALSE = "false";

  public static final String DB_SCHEMA_UPDATE_CREATE = "create";
  
  public static final String DB_SCHEMA_UPDATE_CREATE_DROP = "create-drop";
  
  /**
   * Creates the schema when the form engine is being created and drops the schema when the form engine is being closed.
   */
  public static final String DB_SCHEMA_UPDATE_DROP_CREATE = "drop-create";

  /**
   * Upon building of the process engine, a check is performed and an update of the schema is performed if it is necessary.
   */
  public static final String DB_SCHEMA_UPDATE_TRUE = "true";

  protected String idmEngineName = IdmEngines.NAME_DEFAULT;

  protected String databaseType;
  protected String jdbcDriver = "org.h2.Driver";
  protected String jdbcUrl = "jdbc:h2:tcp://localhost/~/activitiidm";
  protected String jdbcUsername = "sa";
  protected String jdbcPassword = "";
  protected String dataSourceJndiName;
  protected int jdbcMaxActiveConnections;
  protected int jdbcMaxIdleConnections;
  protected int jdbcMaxCheckoutTime;
  protected int jdbcMaxWaitTime;
  protected boolean jdbcPingEnabled;
  protected String jdbcPingQuery;
  protected int jdbcPingConnectionNotUsedFor;
  protected int jdbcDefaultTransactionIsolationLevel;
  protected DataSource dataSource;
  
  protected String databaseSchemaUpdate = DB_SCHEMA_UPDATE_FALSE;

  protected String xmlEncoding = "UTF-8";

  protected BeanFactory beanFactory;

  // COMMAND EXECUTORS ///////////////////////////////////////////////

  protected CommandConfig defaultCommandConfig;
  protected CommandConfig schemaCommandConfig;

  protected CommandInterceptor commandInvoker;

  /**
   * the configurable list which will be {@link #initInterceptorChain(java.util.List) processed} to build the {@link #commandExecutor}
   */
  protected List<CommandInterceptor> customPreCommandInterceptors;
  protected List<CommandInterceptor> customPostCommandInterceptors;

  protected List<CommandInterceptor> commandInterceptors;

  /** this will be initialized during the configurationComplete() */
  protected CommandExecutor commandExecutor;
  
  protected ClassLoader classLoader;
  /**
   * Either use Class.forName or ClassLoader.loadClass for class loading. See http://forums.activiti.org/content/reflectutilloadclass-and-custom- classloader
   */
  protected boolean useClassForNameClassLoading = true;

  // SERVICES
  // /////////////////////////////////////////////////////////////////

  protected IdmIdentityService idmIdentityService = new IdmIdentityServiceImpl();
  protected IdmManagementService idmManagementService = new IdmManagementServiceImpl();
  
  // DATA MANAGERS ///////////////////////////////////////////////////

  protected ByteArrayDataManager byteArrayDataManager;
  protected GroupDataManager groupDataManager;
  protected IdentityInfoDataManager identityInfoDataManager;
  protected MembershipDataManager membershipDataManager;
  protected PropertyDataManager propertyDataManager;
  protected TokenDataManager tokenDataManager;
  protected UserDataManager userDataManager;

  // ENTITY MANAGERS /////////////////////////////////////////////////
  protected ByteArrayEntityManager byteArrayEntityManager;
  protected GroupEntityManager groupEntityManager;
  protected IdentityInfoEntityManager identityInfoEntityManager;
  protected MembershipEntityManager membershipEntityManager;
  protected PropertyEntityManager propertyEntityManager;
  protected TableDataManager tableDataManager;
  protected TokenEntityManager tokenEntityManager;
  protected UserEntityManager userEntityManager;

  protected CommandContextFactory commandContextFactory;
  protected TransactionContextFactory transactionContextFactory;
  
  // MYBATIS SQL SESSION FACTORY /////////////////////////////////////

  protected SqlSessionFactory sqlSessionFactory;
  protected TransactionFactory transactionFactory;

  protected Set<Class<?>> customMybatisMappers;
  protected Set<String> customMybatisXMLMappers;

  // SESSION FACTORIES ///////////////////////////////////////////////
  protected List<SessionFactory> customSessionFactories;
  protected DbSqlSessionFactory dbSqlSessionFactory;
  protected Map<Class<?>, SessionFactory> sessionFactories;
  
  protected boolean enableEventDispatcher = true;
  protected ActivitiIdmEventDispatcher eventDispatcher;
  protected List<ActivitiIdmEventListener> eventListeners;
  protected Map<String, List<ActivitiIdmEventListener>> typedEventListeners;

  protected boolean transactionsExternallyManaged;

  /**
   * Flag that can be set to configure or nota relational database is used. This is useful for custom implementations that do not use relational databases at all.
   * 
   * If true (default), the {@link ProcessEngineConfiguration#getDatabaseSchemaUpdate()} value will be used to determine what needs to happen wrt the database schema.
   * 
   * If false, no validation or schema creation will be done. That means that the database schema must have been created 'manually' before but the engine does not validate whether the schema is
   * correct. The {@link ProcessEngineConfiguration#getDatabaseSchemaUpdate()} value will not be used.
   */
  protected boolean usingRelationalDatabase = true;

  /**
   * Allows configuring a database table prefix which is used for all runtime operations of the process engine. For example, if you specify a prefix named 'PRE1.', activiti will query for executions
   * in a table named 'PRE1.ACT_RU_EXECUTION_'.
   * 
   * <p />
   * <strong>NOTE: the prefix is not respected by automatic database schema management. If you use {@link ProcessEngineConfiguration#DB_SCHEMA_UPDATE_CREATE_DROP} or
   * {@link ProcessEngineConfiguration#DB_SCHEMA_UPDATE_TRUE}, activiti will create the database tables using the default names, regardless of the prefix configured here.</strong>
   */
  protected String databaseTablePrefix = "";
  
  /**
   * Escape character for doing wildcard searches.
   * 
   * This will be added at then end of queries that include for example a LIKE clause.
   * For example: SELECT * FROM table WHERE column LIKE '%\%%' ESCAPE '\';
   */
  protected String databaseWildcardEscapeCharacter;

  /**
   * database catalog to use
   */
  protected String databaseCatalog = "";

  /**
   * In some situations you want to set the schema to use for table checks / generation if the database metadata doesn't return that correctly, see https://jira.codehaus.org/browse/ACT-1220,
   * https://jira.codehaus.org/browse/ACT-1062
   */
  protected String databaseSchema;

  /**
   * Set to true in case the defined databaseTablePrefix is a schema-name, instead of an actual table name prefix. This is relevant for checking if Activiti-tables exist, the databaseTablePrefix will
   * not be used here - since the schema is taken into account already, adding a prefix for the table-check will result in wrong table-names.
   */
  protected boolean tablePrefixIsSchema;

  protected static Properties databaseTypeMappings = getDefaultDatabaseTypeMappings();

  public static final String DATABASE_TYPE_H2 = "h2";
  public static final String DATABASE_TYPE_HSQL = "hsql";
  public static final String DATABASE_TYPE_MYSQL = "mysql";
  public static final String DATABASE_TYPE_ORACLE = "oracle";
  public static final String DATABASE_TYPE_POSTGRES = "postgres";
  public static final String DATABASE_TYPE_MSSQL = "mssql";
  public static final String DATABASE_TYPE_DB2 = "db2";

  public static Properties getDefaultDatabaseTypeMappings() {
    Properties databaseTypeMappings = new Properties();
    databaseTypeMappings.setProperty("H2", DATABASE_TYPE_H2);
    databaseTypeMappings.setProperty("HSQL Database Engine", DATABASE_TYPE_HSQL);
    databaseTypeMappings.setProperty("MySQL", DATABASE_TYPE_MYSQL);
    databaseTypeMappings.setProperty("Oracle", DATABASE_TYPE_ORACLE);
    databaseTypeMappings.setProperty("PostgreSQL", DATABASE_TYPE_POSTGRES);
    databaseTypeMappings.setProperty("Microsoft SQL Server", DATABASE_TYPE_MSSQL);
    databaseTypeMappings.setProperty(DATABASE_TYPE_DB2, DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/NT", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/NT64", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2 UDP", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/LINUX", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/LINUX390", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/LINUXX8664", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/LINUXZ64", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/LINUXPPC64", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/400 SQL", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/6000", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2 UDB iSeries", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/AIX64", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/HPUX", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/HP64", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/SUN", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/SUN64", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/PTX", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/2", DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2 UDB AS400", DATABASE_TYPE_DB2);
    return databaseTypeMappings;
  }

  protected IdGenerator idGenerator;

  protected Clock clock;

  public static IdmEngineConfiguration createIdmEngineConfigurationFromResourceDefault() {
    return createIdmEngineConfigurationFromResource("activiti.idm.cfg.xml", "idmEngineConfiguration");
  }

  public static IdmEngineConfiguration createIdmEngineConfigurationFromResource(String resource) {
    return createIdmEngineConfigurationFromResource(resource, "idmEngineConfiguration");
  }

  public static IdmEngineConfiguration createIdmEngineConfigurationFromResource(String resource, String beanName) {
    return parseIdmEngineConfigurationFromResource(resource, beanName);
  }

  public static IdmEngineConfiguration createIdmEngineConfigurationFromInputStream(InputStream inputStream) {
    return createIdmEngineConfigurationFromInputStream(inputStream, "idmEngineConfiguration");
  }

  public static IdmEngineConfiguration createIdmEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
    return parseIdmEngineConfigurationFromInputStream(inputStream, beanName);
  }

  public static IdmEngineConfiguration createStandaloneIdmEngineConfiguration() {
    return new StandaloneIdmEngineConfiguration();
  }

  public static IdmEngineConfiguration createStandaloneInMemIdmEngineConfiguration() {
    return new StandaloneInMemIdmEngineConfiguration();
  }

  public static IdmEngineConfiguration parseIdmEngineConfiguration(Resource springResource, String beanName) {
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
    xmlBeanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
    xmlBeanDefinitionReader.loadBeanDefinitions(springResource);
    IdmEngineConfiguration formEngineConfiguration = (IdmEngineConfiguration) beanFactory.getBean(beanName);
    formEngineConfiguration.setBeanFactory(beanFactory);
    return formEngineConfiguration;
  }

  public static IdmEngineConfiguration parseIdmEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
    Resource springResource = new InputStreamResource(inputStream);
    return parseIdmEngineConfiguration(springResource, beanName);
  }

  public static IdmEngineConfiguration parseIdmEngineConfigurationFromResource(String resource, String beanName) {
    Resource springResource = new ClassPathResource(resource);
    return parseIdmEngineConfiguration(springResource, beanName);
  }

  // buildProcessEngine
  // ///////////////////////////////////////////////////////

  public IdmEngine buildIdmEngine() {
    init();
    return new IdmEngineImpl(this);
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
    }
    
    initTransactionFactory();
    initSqlSessionFactory();
    initSessionFactories();
    initServices();
    initDataManagers();
    initEntityManagers();
    initClock();
    initEventDispatcher();
  }

  // services
  // /////////////////////////////////////////////////////////////////

  protected void initServices() {
    initService(idmIdentityService);
    initService(idmManagementService);
  }

  protected void initService(Object service) {
    if (service instanceof ServiceImpl) {
      ((ServiceImpl) service).setCommandExecutor(commandExecutor);
    }
  }
  
  // Data managers
  ///////////////////////////////////////////////////////////

  public void initDataManagers() {
    if (byteArrayDataManager == null) {
      byteArrayDataManager = new MybatisByteArrayDataManager(this);
    }
    if (groupDataManager == null) {
    	groupDataManager = new MybatisGroupDataManager(this);
    }
    if (identityInfoDataManager == null) {
    	identityInfoDataManager = new MybatisIdentityInfoDataManager(this);
    }
    if (membershipDataManager == null) {
    	membershipDataManager = new MybatisMembershipDataManager(this);
    }
    if (propertyDataManager == null) {
      propertyDataManager = new MybatisPropertyDataManager(this);
    }
    if (tokenDataManager == null) {
      tokenDataManager = new MybatisTokenDataManager(this);
    }
    if (userDataManager == null) {
    	userDataManager = new MybatisUserDataManager(this);
    }
  }

  public void initEntityManagers() {
    if (byteArrayEntityManager == null) {
      byteArrayEntityManager = new ByteArrayEntityManagerImpl(this, byteArrayDataManager);
    }
    if (groupEntityManager == null) {
    	groupEntityManager = new GroupEntityManagerImpl(this, groupDataManager);
    }
    if (identityInfoEntityManager == null) {
    	identityInfoEntityManager = new IdentityInfoEntityManagerImpl(this, identityInfoDataManager);
    }
    if (membershipEntityManager == null) {
    	membershipEntityManager = new MembershipEntityManagerImpl(this, membershipDataManager);
    }
    if (propertyEntityManager == null) {
      propertyEntityManager = new PropertyEntityManagerImpl(this, propertyDataManager);
    }
    if (tableDataManager == null) {
      tableDataManager = new TableDataManagerImpl(this);
    }
    if (tokenEntityManager == null) {
      tokenEntityManager = new TokenEntityManagerImpl(this, tokenDataManager);
    }
    if (userEntityManager == null) {
      userEntityManager = new UserEntityManagerImpl(this, userDataManager);
    }
  }

  // DataSource
  // ///////////////////////////////////////////////////////////////

  protected void initDataSource() {
    if (dataSource == null) {
      if (dataSourceJndiName != null) {
        try {
          dataSource = (DataSource) new InitialContext().lookup(dataSourceJndiName);
        } catch (Exception e) {
          throw new ActivitiIdmException("couldn't lookup datasource from " + dataSourceJndiName + ": " + e.getMessage(), e);
        }

      } else if (jdbcUrl != null) {
        if ((jdbcDriver == null) || (jdbcUsername == null)) {
          throw new ActivitiIdmException("DataSource or JDBC properties have to be specified in a process engine configuration");
        }

        logger.debug("initializing datasource to db: {}", jdbcUrl);

        if (logger.isInfoEnabled()) {
          logger.info("Configuring Datasource with following properties (omitted password for security)");
          logger.info("datasource driver: " + jdbcDriver);
          logger.info("datasource url : " + jdbcUrl);
          logger.info("datasource user name : " + jdbcUsername);
        }

        PooledDataSource pooledDataSource = new PooledDataSource(this.getClass().getClassLoader(), jdbcDriver, jdbcUrl, jdbcUsername, jdbcPassword);

        if (jdbcMaxActiveConnections > 0) {
          pooledDataSource.setPoolMaximumActiveConnections(jdbcMaxActiveConnections);
        }
        if (jdbcMaxIdleConnections > 0) {
          pooledDataSource.setPoolMaximumIdleConnections(jdbcMaxIdleConnections);
        }
        if (jdbcMaxCheckoutTime > 0) {
          pooledDataSource.setPoolMaximumCheckoutTime(jdbcMaxCheckoutTime);
        }
        if (jdbcMaxWaitTime > 0) {
          pooledDataSource.setPoolTimeToWait(jdbcMaxWaitTime);
        }
        if (jdbcPingEnabled == true) {
          pooledDataSource.setPoolPingEnabled(true);
          if (jdbcPingQuery != null) {
            pooledDataSource.setPoolPingQuery(jdbcPingQuery);
          }
          pooledDataSource.setPoolPingConnectionsNotUsedFor(jdbcPingConnectionNotUsedFor);
        }
        if (jdbcDefaultTransactionIsolationLevel > 0) {
          pooledDataSource.setDefaultTransactionIsolationLevel(jdbcDefaultTransactionIsolationLevel);
        }
        dataSource = pooledDataSource;
      }
      
      if (dataSource instanceof PooledDataSource) {
        // ACT-233: connection pool of Ibatis is not properly
        // initialized if this is not called!
        ((PooledDataSource) dataSource).forceCloseAll();
      }
    }
    
    if (databaseType == null) {
      initDatabaseType();
    }
  }
  
  public void initDatabaseType() {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      DatabaseMetaData databaseMetaData = connection.getMetaData();
      String databaseProductName = databaseMetaData.getDatabaseProductName();
      logger.debug("database product name: '{}'", databaseProductName);
      databaseType = databaseTypeMappings.getProperty(databaseProductName);
      if (databaseType == null) {
        throw new ActivitiIdmException("couldn't deduct database type from database product name '" + databaseProductName + "'");
      }
      logger.debug("using database type: {}", databaseType);

    } catch (SQLException e) {
      logger.error("Exception while initializing Database connection", e);
    } finally {
      try {
        if (connection != null) {
          connection.close();
        }
      } catch (SQLException e) {
        logger.error("Exception while closing the Database connection", e);
      }
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

  public void addSessionFactory(SessionFactory sessionFactory) {
    sessionFactories.put(sessionFactory.getSessionType(), sessionFactory);
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

  public void initDefaultCommandConfig() {
    if (defaultCommandConfig == null) {
      defaultCommandConfig = new CommandConfig();
    }
  }

  public void initSchemaCommandConfig() {
    if (schemaCommandConfig == null) {
      schemaCommandConfig = new CommandConfig().transactionNotSupported();
    }
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

    CommandInterceptor transactionInterceptor = createTransactionInterceptor();
    if (transactionInterceptor != null) {
      interceptors.add(transactionInterceptor);
    }

    interceptors.add(new CommandContextInterceptor(commandContextFactory, this));
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
      throw new ActivitiIdmException("invalid command interceptor chain configuration: " + chain);
    }
    for (int i = 0; i < chain.size() - 1; i++) {
      chain.get(i).setNext(chain.get(i + 1));
    }
    return chain.get(0);
  }

  public CommandInterceptor createTransactionInterceptor() {
    return null;
  }

  // id generator
  // /////////////////////////////////////////////////////////////

  public void initIdGenerator() {
    if (idGenerator == null) {
      idGenerator = new StrongUuidGenerator();
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

  public void initClock() {
    if (clock == null) {
      clock = new DefaultClockImpl();
    }
  }

  // myBatis SqlSessionFactory
  // ////////////////////////////////////////////////

  public void initTransactionFactory() {
    if (transactionFactory == null) {
      if (transactionsExternallyManaged) {
        transactionFactory = new ManagedTransactionFactory();
      } else {
        transactionFactory = new JdbcTransactionFactory();
      }
    }
  }

  public void initSqlSessionFactory() {
    if (sqlSessionFactory == null) {
      InputStream inputStream = null;
      try {
        inputStream = getMyBatisXmlConfigurationStream();

        Environment environment = new Environment("default", transactionFactory, dataSource);
        Reader reader = new InputStreamReader(inputStream);
        Properties properties = new Properties();
        properties.put("prefix", databaseTablePrefix);
        
        String wildcardEscapeClause = "";
        if ((databaseWildcardEscapeCharacter != null) && (databaseWildcardEscapeCharacter.length() != 0)) {
          wildcardEscapeClause = " escape '" + databaseWildcardEscapeCharacter + "'";
        }
        properties.put("wildcardEscapeClause", wildcardEscapeClause);
        
        // set default properties
        properties.put("limitBefore", "");
        properties.put("limitAfter", "");
        properties.put("limitBetween", "");
        properties.put("limitOuterJoinBetween", "");
        properties.put("limitBeforeNativeQuery", "");
        properties.put("orderBy", "order by ${orderByColumns}");
        properties.put("blobType", "BLOB");
        properties.put("boolValue", "TRUE");

        if (databaseType != null) {
          properties.load(getResourceAsStream("org/activiti/idm/db/properties/" + databaseType + ".properties"));
        }

        Configuration configuration = initMybatisConfiguration(environment, reader, properties);
        sqlSessionFactory = new DefaultSqlSessionFactory(configuration);

      } catch (Exception e) {
        throw new ActivitiIdmException("Error while building ibatis SqlSessionFactory: " + e.getMessage(), e);
      } finally {
        IOUtils.closeQuietly(inputStream);
      }
    }
  }

  public Configuration initMybatisConfiguration(Environment environment, Reader reader, Properties properties) {
    XMLConfigBuilder parser = new XMLConfigBuilder(reader, "", properties);
    Configuration configuration = parser.getConfiguration();

    if (databaseType != null) {
      configuration.setDatabaseId(databaseType);
    }

    configuration.setEnvironment(environment);

    initCustomMybatisMappers(configuration);

    configuration = parseMybatisConfiguration(configuration, parser);
    return configuration;
  }

  public void initCustomMybatisMappers(Configuration configuration) {
    if (getCustomMybatisMappers() != null) {
      for (Class<?> clazz : getCustomMybatisMappers()) {
        configuration.addMapper(clazz);
      }
    }
  }

  public Configuration parseMybatisConfiguration(Configuration configuration, XMLConfigBuilder parser) {
    return parseCustomMybatisXMLMappers(parser.parse());
  }

  public Configuration parseCustomMybatisXMLMappers(Configuration configuration) {
    if (getCustomMybatisXMLMappers() != null)
      // see XMLConfigBuilder.mapperElement()
      for (String resource : getCustomMybatisXMLMappers()) {
      XMLMapperBuilder mapperParser = new XMLMapperBuilder(getResourceAsStream(resource), configuration, resource, configuration.getSqlFragments());
      mapperParser.parse();
      }
    return configuration;
  }

  protected InputStream getResourceAsStream(String resource) {
    return this.getClass().getClassLoader().getResourceAsStream(resource);
  }

  public InputStream getMyBatisXmlConfigurationStream() {
    return getResourceAsStream(DEFAULT_MYBATIS_MAPPING_FILE);
  }
  
  public void initEventDispatcher() {
    if (this.eventDispatcher == null) {
      this.eventDispatcher = new ActivitiIdmEventDispatcherImpl();
    }

    this.eventDispatcher.setEnabled(enableEventDispatcher);

    if (eventListeners != null) {
      for (ActivitiIdmEventListener listenerToAdd : eventListeners) {
        this.eventDispatcher.addEventListener(listenerToAdd);
      }
    }

    if (typedEventListeners != null) {
      for (Entry<String, List<ActivitiIdmEventListener>> listenersToAdd : typedEventListeners.entrySet()) {
        // Extract types from the given string
        ActivitiIdmEventType[] types = ActivitiIdmEventType.getTypesFromString(listenersToAdd.getKey());

        for (ActivitiIdmEventListener listenerToAdd : listenersToAdd.getValue()) {
          this.eventDispatcher.addEventListener(listenerToAdd, types);
        }
      }
    }

  }

  // getters and setters
  // //////////////////////////////////////////////////////

  public String getIdmEngineName() {
    return idmEngineName;
  }

  public IdmEngineConfiguration setIdmEngineName(String idmEngineName) {
    this.idmEngineName = idmEngineName;
    return this;
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public IdmEngineConfiguration setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  public boolean isUseClassForNameClassLoading() {
    return useClassForNameClassLoading;
  }

  public IdmEngineConfiguration setUseClassForNameClassLoading(boolean useClassForNameClassLoading) {
    this.useClassForNameClassLoading = useClassForNameClassLoading;
    return this;
  }

  public String getDatabaseType() {
    return databaseType;
  }

  public IdmEngineConfiguration setDatabaseType(String databaseType) {
    this.databaseType = databaseType;
    return this;
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  public IdmEngineConfiguration setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
    return this;
  }

  public String getJdbcDriver() {
    return jdbcDriver;
  }

  public IdmEngineConfiguration setJdbcDriver(String jdbcDriver) {
    this.jdbcDriver = jdbcDriver;
    return this;
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public IdmEngineConfiguration setJdbcUrl(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
    return this;
  }

  public String getJdbcUsername() {
    return jdbcUsername;
  }

  public IdmEngineConfiguration setJdbcUsername(String jdbcUsername) {
    this.jdbcUsername = jdbcUsername;
    return this;
  }

  public String getJdbcPassword() {
    return jdbcPassword;
  }

  public IdmEngineConfiguration setJdbcPassword(String jdbcPassword) {
    this.jdbcPassword = jdbcPassword;
    return this;
  }

  public int getJdbcMaxActiveConnections() {
    return jdbcMaxActiveConnections;
  }

  public IdmEngineConfiguration setJdbcMaxActiveConnections(int jdbcMaxActiveConnections) {
    this.jdbcMaxActiveConnections = jdbcMaxActiveConnections;
    return this;
  }

  public int getJdbcMaxIdleConnections() {
    return jdbcMaxIdleConnections;
  }

  public IdmEngineConfiguration setJdbcMaxIdleConnections(int jdbcMaxIdleConnections) {
    this.jdbcMaxIdleConnections = jdbcMaxIdleConnections;
    return this;
  }

  public int getJdbcMaxCheckoutTime() {
    return jdbcMaxCheckoutTime;
  }

  public IdmEngineConfiguration setJdbcMaxCheckoutTime(int jdbcMaxCheckoutTime) {
    this.jdbcMaxCheckoutTime = jdbcMaxCheckoutTime;
    return this;
  }

  public int getJdbcMaxWaitTime() {
    return jdbcMaxWaitTime;
  }

  public IdmEngineConfiguration setJdbcMaxWaitTime(int jdbcMaxWaitTime) {
    this.jdbcMaxWaitTime = jdbcMaxWaitTime;
    return this;
  }

  public boolean isJdbcPingEnabled() {
    return jdbcPingEnabled;
  }

  public IdmEngineConfiguration setJdbcPingEnabled(boolean jdbcPingEnabled) {
    this.jdbcPingEnabled = jdbcPingEnabled;
    return this;
  }

  public int getJdbcPingConnectionNotUsedFor() {
    return jdbcPingConnectionNotUsedFor;
  }

  public IdmEngineConfiguration setJdbcPingConnectionNotUsedFor(int jdbcPingConnectionNotUsedFor) {
    this.jdbcPingConnectionNotUsedFor = jdbcPingConnectionNotUsedFor;
    return this;
  }

  public int getJdbcDefaultTransactionIsolationLevel() {
    return jdbcDefaultTransactionIsolationLevel;
  }

  public IdmEngineConfiguration setJdbcDefaultTransactionIsolationLevel(int jdbcDefaultTransactionIsolationLevel) {
    this.jdbcDefaultTransactionIsolationLevel = jdbcDefaultTransactionIsolationLevel;
    return this;
  }

  public String getJdbcPingQuery() {
    return jdbcPingQuery;
  }

  public IdmEngineConfiguration setJdbcPingQuery(String jdbcPingQuery) {
    this.jdbcPingQuery = jdbcPingQuery;
    return this;
  }

  public String getDataSourceJndiName() {
    return dataSourceJndiName;
  }

  public IdmEngineConfiguration setDataSourceJndiName(String dataSourceJndiName) {
    this.dataSourceJndiName = dataSourceJndiName;
    return this;
  }

  public CommandConfig getSchemaCommandConfig() {
    return schemaCommandConfig;
  }

  public IdmEngineConfiguration setSchemaCommandConfig(CommandConfig schemaCommandConfig) {
    this.schemaCommandConfig = schemaCommandConfig;
    return this;
  }

  public boolean isTransactionsExternallyManaged() {
    return transactionsExternallyManaged;
  }

  public IdmEngineConfiguration setTransactionsExternallyManaged(boolean transactionsExternallyManaged) {
    this.transactionsExternallyManaged = transactionsExternallyManaged;
    return this;
  }

  public IdGenerator getIdGenerator() {
    return idGenerator;
  }

  public IdmEngineConfiguration setIdGenerator(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
    return this;
  }

  public String getXmlEncoding() {
    return xmlEncoding;
  }

  public IdmEngineConfiguration setXmlEncoding(String xmlEncoding) {
    this.xmlEncoding = xmlEncoding;
    return this;
  }

  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public IdmEngineConfiguration setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    return this;
  }

  public CommandConfig getDefaultCommandConfig() {
    return defaultCommandConfig;
  }

  public IdmEngineConfiguration setDefaultCommandConfig(CommandConfig defaultCommandConfig) {
    this.defaultCommandConfig = defaultCommandConfig;
    return this;
  }

  public CommandInterceptor getCommandInvoker() {
    return commandInvoker;
  }

  public IdmEngineConfiguration setCommandInvoker(CommandInterceptor commandInvoker) {
    this.commandInvoker = commandInvoker;
    return this;
  }

  public List<CommandInterceptor> getCustomPreCommandInterceptors() {
    return customPreCommandInterceptors;
  }

  public IdmEngineConfiguration setCustomPreCommandInterceptors(List<CommandInterceptor> customPreCommandInterceptors) {
    this.customPreCommandInterceptors = customPreCommandInterceptors;
    return this;
  }

  public List<CommandInterceptor> getCustomPostCommandInterceptors() {
    return customPostCommandInterceptors;
  }

  public IdmEngineConfiguration setCustomPostCommandInterceptors(List<CommandInterceptor> customPostCommandInterceptors) {
    this.customPostCommandInterceptors = customPostCommandInterceptors;
    return this;
  }

  public List<CommandInterceptor> getCommandInterceptors() {
    return commandInterceptors;
  }

  public IdmEngineConfiguration setCommandInterceptors(List<CommandInterceptor> commandInterceptors) {
    this.commandInterceptors = commandInterceptors;
    return this;
  }

  public CommandExecutor getCommandExecutor() {
    return commandExecutor;
  }

  public IdmEngineConfiguration setCommandExecutor(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
    return this;
  }

  public IdmIdentityService getIdmIdentityService() {
    return idmIdentityService;
  }
  
  public IdmEngineConfiguration setIdmIdentityService(IdmIdentityService idmIdentityService) {
    this.idmIdentityService = idmIdentityService;
    return this;
  }
  
  public IdmManagementService getIdmManagementService() {
    return idmManagementService;
  }
  
  public IdmEngineConfiguration setIdmManagementService(IdmManagementService idmManagementService) {
    this.idmManagementService = idmManagementService;
    return this;
  }

  public IdmEngineConfiguration getIdmEngineConfiguration() {
    return this;
  }

  public ByteArrayDataManager getByteArrayDataManager() {
    return byteArrayDataManager;
  }

  public IdmEngineConfiguration setByteArrayDataManager(ByteArrayDataManager byteArrayDataManager) {
    this.byteArrayDataManager = byteArrayDataManager;
    return this;
  }

  public GroupDataManager getGroupDataManager() {
    return groupDataManager;
  }

  public IdmEngineConfiguration setGroupDataManager(GroupDataManager groupDataManager) {
    this.groupDataManager = groupDataManager;
    return this;
  }
  
  public IdentityInfoDataManager getIdentityInfoDataManager() {
    return identityInfoDataManager;
  }

  public IdmEngineConfiguration setIdentityInfoDataManager(IdentityInfoDataManager identityInfoDataManager) {
    this.identityInfoDataManager = identityInfoDataManager;
    return this;
  }
  
  public MembershipDataManager getMembershipDataManager() {
    return membershipDataManager;
  }

  public IdmEngineConfiguration setMembershipDataManager(MembershipDataManager membershipDataManager) {
    this.membershipDataManager = membershipDataManager;
    return this;
  }
  
  public PropertyDataManager getPropertyDataManager() {
    return propertyDataManager;
  }

  public IdmEngineConfiguration setPropertyDataManager(PropertyDataManager propertyDataManager) {
    this.propertyDataManager = propertyDataManager;
    return this;
  }

  public TokenDataManager getTokenDataManager() {
    return tokenDataManager;
  }

  public IdmEngineConfiguration setTokenDataManager(TokenDataManager tokenDataManager) {
    this.tokenDataManager = tokenDataManager;
    return this;
  }

  public UserDataManager getUserDataManager() {
    return userDataManager;
  }

  public IdmEngineConfiguration setUserDataManager(UserDataManager userDataManager) {
    this.userDataManager = userDataManager;
    return this;
  }

  public ByteArrayEntityManager getByteArrayEntityManager() {
    return byteArrayEntityManager;
  }

  public IdmEngineConfiguration setByteArrayEntityManager(ByteArrayEntityManager byteArrayEntityManager) {
    this.byteArrayEntityManager = byteArrayEntityManager;
    return this;
  }

  public GroupEntityManager getGroupEntityManager() {
    return groupEntityManager;
  }

  public IdmEngineConfiguration setGroupEntityManager(GroupEntityManager groupEntityManager) {
    this.groupEntityManager = groupEntityManager;
    return this;
  }

  public IdentityInfoEntityManager getIdentityInfoEntityManager() {
    return identityInfoEntityManager;
  }

  public IdmEngineConfiguration setIdentityInfoEntityManager(IdentityInfoEntityManager identityInfoEntityManager) {
    this.identityInfoEntityManager = identityInfoEntityManager;
    return this;
  }
  
  public MembershipEntityManager getMembershipEntityManager() {
    return membershipEntityManager;
  }

  public IdmEngineConfiguration setMembershipEntityManager(MembershipEntityManager membershipEntityManager) {
    this.membershipEntityManager = membershipEntityManager;
    return this;
  }
  
  public PropertyEntityManager getPropertyEntityManager() {
    return propertyEntityManager;
  }

  public IdmEngineConfiguration setPropertyEntityManager(PropertyEntityManager propertyEntityManager) {
    this.propertyEntityManager = propertyEntityManager;
    return this;
  }

  public TokenEntityManager getTokenEntityManager() {
    return tokenEntityManager;
  }

  public IdmEngineConfiguration setTokenEntityManager(TokenEntityManager tokenEntityManager) {
    this.tokenEntityManager = tokenEntityManager;
    return this;
  }

  public UserEntityManager getUserEntityManager() {
    return userEntityManager;
  }

  public IdmEngineConfiguration setUserEntityManager(UserEntityManager userEntityManager) {
    this.userEntityManager = userEntityManager;
    return this;
  }
  
  public TableDataManager getTableDataManager() {
    return tableDataManager;
  }

  public IdmEngineConfiguration setTableDataManager(TableDataManager tableDataManager) {
    this.tableDataManager = tableDataManager;
    return this;
  }

  public CommandContextFactory getCommandContextFactory() {
    return commandContextFactory;
  }

  public IdmEngineConfiguration setCommandContextFactory(CommandContextFactory commandContextFactory) {
    this.commandContextFactory = commandContextFactory;
    return this;
  }

  public SqlSessionFactory getSqlSessionFactory() {
    return sqlSessionFactory;
  }

  public IdmEngineConfiguration setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    return this;
  }

  public TransactionFactory getTransactionFactory() {
    return transactionFactory;
  }

  public IdmEngineConfiguration setTransactionFactory(TransactionFactory transactionFactory) {
    this.transactionFactory = transactionFactory;
    return this;
  }

  public Set<Class<?>> getCustomMybatisMappers() {
    return customMybatisMappers;
  }

  public IdmEngineConfiguration setCustomMybatisMappers(Set<Class<?>> customMybatisMappers) {
    this.customMybatisMappers = customMybatisMappers;
    return this;
  }

  public Set<String> getCustomMybatisXMLMappers() {
    return customMybatisXMLMappers;
  }

  public IdmEngineConfiguration setCustomMybatisXMLMappers(Set<String> customMybatisXMLMappers) {
    this.customMybatisXMLMappers = customMybatisXMLMappers;
    return this;
  }

  public List<SessionFactory> getCustomSessionFactories() {
    return customSessionFactories;
  }

  public IdmEngineConfiguration setCustomSessionFactories(List<SessionFactory> customSessionFactories) {
    this.customSessionFactories = customSessionFactories;
    return this;
  }

  public DbSqlSessionFactory getDbSqlSessionFactory() {
    return dbSqlSessionFactory;
  }

  public IdmEngineConfiguration setDbSqlSessionFactory(DbSqlSessionFactory dbSqlSessionFactory) {
    this.dbSqlSessionFactory = dbSqlSessionFactory;
    return this;
  }

  public boolean isUsingRelationalDatabase() {
    return usingRelationalDatabase;
  }

  public IdmEngineConfiguration setUsingRelationalDatabase(boolean usingRelationalDatabase) {
    this.usingRelationalDatabase = usingRelationalDatabase;
    return this;
  }

  public String getDatabaseTablePrefix() {
    return databaseTablePrefix;
  }

  public IdmEngineConfiguration setDatabaseTablePrefix(String databaseTablePrefix) {
    this.databaseTablePrefix = databaseTablePrefix;
    return this;
  }

  public String getDatabaseWildcardEscapeCharacter() {
    return databaseWildcardEscapeCharacter;
  }

  public IdmEngineConfiguration setDatabaseWildcardEscapeCharacter(String databaseWildcardEscapeCharacter) {
    this.databaseWildcardEscapeCharacter = databaseWildcardEscapeCharacter;
    return this;
  }

  public String getDatabaseCatalog() {
    return databaseCatalog;
  }

  public IdmEngineConfiguration setDatabaseCatalog(String databaseCatalog) {
    this.databaseCatalog = databaseCatalog;
    return this;
  }

  public String getDatabaseSchema() {
    return databaseSchema;
  }

  public IdmEngineConfiguration setDatabaseSchema(String databaseSchema) {
    this.databaseSchema = databaseSchema;
    return this;
  }

  public boolean isTablePrefixIsSchema() {
    return tablePrefixIsSchema;
  }

  public IdmEngineConfiguration setTablePrefixIsSchema(boolean tablePrefixIsSchema) {
    this.tablePrefixIsSchema = tablePrefixIsSchema;
    return this;
  }

  public Map<Class<?>, SessionFactory> getSessionFactories() {
    return sessionFactories;
  }

  public IdmEngineConfiguration setSessionFactories(Map<Class<?>, SessionFactory> sessionFactories) {
    this.sessionFactories = sessionFactories;
    return this;
  }

  public TransactionContextFactory getTransactionContextFactory() {
    return transactionContextFactory;
  }

  public IdmEngineConfiguration setTransactionContextFactory(TransactionContextFactory transactionContextFactory) {
    this.transactionContextFactory = transactionContextFactory;
    return this;
  }
  
  public String getDatabaseSchemaUpdate() {
    return databaseSchemaUpdate;
  }
  
  public IdmEngineConfiguration setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
    this.databaseSchemaUpdate = databaseSchemaUpdate;
    return this;
  }

  public boolean isEnableEventDispatcher() {
    return enableEventDispatcher;
  }

  public IdmEngineConfiguration setEnableEventDispatcher(boolean enableEventDispatcher) {
    this.enableEventDispatcher = enableEventDispatcher;
    return this;
  }

  public ActivitiIdmEventDispatcher getEventDispatcher() {
    return eventDispatcher;
  }

  public IdmEngineConfiguration setEventDispatcher(ActivitiIdmEventDispatcher eventDispatcher) {
    this.eventDispatcher = eventDispatcher;
    return this;
  }

  public List<ActivitiIdmEventListener> getEventListeners() {
    return eventListeners;
  }

  public IdmEngineConfiguration setEventListeners(List<ActivitiIdmEventListener> eventListeners) {
    this.eventListeners = eventListeners;
    return this;
  }

  public Map<String, List<ActivitiIdmEventListener>> getTypedEventListeners() {
    return typedEventListeners;
  }

  public IdmEngineConfiguration setTypedEventListeners(Map<String, List<ActivitiIdmEventListener>> typedEventListeners) {
    this.typedEventListeners = typedEventListeners;
    return this;
  }

  public Clock getClock() {
    return clock;
  }

  public IdmEngineConfiguration setClock(Clock clock) {
    this.clock = clock;
    return this;
  }
}
