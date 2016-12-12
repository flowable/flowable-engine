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
package org.flowable.engine.common;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.InitialContext;
import javax.sql.DataSource;

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
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.delegate.event.FlowableEventDispatcher;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.common.impl.cfg.IdGenerator;
import org.flowable.engine.common.impl.interceptor.CommandConfig;
import org.flowable.engine.common.impl.interceptor.SessionFactory;
import org.flowable.engine.common.impl.persistence.StrongUuidGenerator;
import org.flowable.engine.common.impl.util.DefaultClockImpl;
import org.flowable.engine.common.impl.util.IoUtil;
import org.flowable.engine.common.runtime.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEngineConfiguration {

  protected static final Logger logger = LoggerFactory.getLogger(AbstractEngineConfiguration.class);

  /** The tenant id indicating 'no tenant' */
  public static final String NO_TENANT_ID = "";

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

  protected String databaseType;
  protected String jdbcDriver = "org.h2.Driver";
  protected String jdbcUrl = "jdbc:h2:tcp://localhost/~/activiti";
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

  // COMMAND EXECUTORS ///////////////////////////////////////////////

  protected CommandConfig defaultCommandConfig;
  protected CommandConfig schemaCommandConfig;
  
  protected ClassLoader classLoader;
  /**
   * Either use Class.forName or ClassLoader.loadClass for class loading. See http://forums.activiti.org/content/reflectutilloadclass-and-custom- classloader
   */
  protected boolean useClassForNameClassLoading = true;
  
  // MYBATIS SQL SESSION FACTORY /////////////////////////////////////

  protected SqlSessionFactory sqlSessionFactory;
  protected TransactionFactory transactionFactory;

  protected Set<Class<?>> customMybatisMappers;
  protected Set<String> customMybatisXMLMappers;

  // SESSION FACTORIES ///////////////////////////////////////////////
  protected List<SessionFactory> customSessionFactories;
  protected Map<Class<?>, SessionFactory> sessionFactories;
  
  protected boolean enableEventDispatcher = true;
  protected FlowableEventDispatcher eventDispatcher;
  protected List<FlowableEventListener> eventListeners;
  protected Map<String, List<FlowableEventListener>> typedEventListeners;

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
   * Set to true in case the defined databaseTablePrefix is a schema-name, instead of an actual table name prefix. This is relevant for checking if Flowable-tables exist, the databaseTablePrefix will
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
  
  protected Map<Object, Object> beans;

  protected IdGenerator idGenerator;

  protected Clock clock;

  // DataSource
  // ///////////////////////////////////////////////////////////////

  protected void initDataSource() {
    if (dataSource == null) {
      if (dataSourceJndiName != null) {
        try {
          dataSource = (DataSource) new InitialContext().lookup(dataSourceJndiName);
        } catch (Exception e) {
          throw new FlowableException("couldn't lookup datasource from " + dataSourceJndiName + ": " + e.getMessage(), e);
        }

      } else if (jdbcUrl != null) {
        if ((jdbcDriver == null) || (jdbcUsername == null)) {
          throw new FlowableException("DataSource or JDBC properties have to be specified in a process engine configuration");
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
        throw new FlowableException("couldn't deduct database type from database product name '" + databaseProductName + '\'');
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

  public void addSessionFactory(SessionFactory sessionFactory) {
    sessionFactories.put(sessionFactory.getSessionType(), sessionFactory);
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
  
  public void initBeans() {
    if (beans == null) {
      beans = new HashMap<Object, Object>();
    }
  }

  // id generator
  // /////////////////////////////////////////////////////////////

  public void initIdGenerator() {
    if (idGenerator == null) {
      idGenerator = new StrongUuidGenerator();
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
        Properties properties = new Properties();
        properties.put("closeConnection", "false");
        this.transactionFactory.setProperties(properties);
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
          wildcardEscapeClause = " escape '" + databaseWildcardEscapeCharacter + '\'';
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
          properties.load(getResourceAsStream(pathToEngineDbProperties()));
        }

        Configuration configuration = initMybatisConfiguration(environment, reader, properties);
        sqlSessionFactory = new DefaultSqlSessionFactory(configuration);

      } catch (Exception e) {
        throw new FlowableException("Error while building ibatis SqlSessionFactory: " + e.getMessage(), e);
      } finally {
        IoUtil.closeSilently(inputStream);
      }
    }
  }
  
  public abstract String pathToEngineDbProperties();

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
    if (getCustomMybatisXMLMappers() != null) {
      // see XMLConfigBuilder.mapperElement()
      for (String resource : getCustomMybatisXMLMappers()) {
        XMLMapperBuilder mapperParser = new XMLMapperBuilder(getResourceAsStream(resource), configuration, resource, configuration.getSqlFragments());
        mapperParser.parse();
      }
    }
    return configuration;
  }

  protected InputStream getResourceAsStream(String resource) {
    return this.getClass().getClassLoader().getResourceAsStream(resource);
  }

  public abstract InputStream getMyBatisXmlConfigurationStream();

  // getters and setters
  // //////////////////////////////////////////////////////

  public abstract String getEngineName();

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public AbstractEngineConfiguration setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }
  
  public boolean isUseClassForNameClassLoading() {
    return useClassForNameClassLoading;
  }

  public AbstractEngineConfiguration setUseClassForNameClassLoading(boolean useClassForNameClassLoading) {
    this.useClassForNameClassLoading = useClassForNameClassLoading;
    return this;
  }

  public String getDatabaseType() {
    return databaseType;
  }

  public AbstractEngineConfiguration setDatabaseType(String databaseType) {
    this.databaseType = databaseType;
    return this;
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  public AbstractEngineConfiguration setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
    return this;
  }

  public String getJdbcDriver() {
    return jdbcDriver;
  }

  public AbstractEngineConfiguration setJdbcDriver(String jdbcDriver) {
    this.jdbcDriver = jdbcDriver;
    return this;
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public AbstractEngineConfiguration setJdbcUrl(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
    return this;
  }

  public String getJdbcUsername() {
    return jdbcUsername;
  }

  public AbstractEngineConfiguration setJdbcUsername(String jdbcUsername) {
    this.jdbcUsername = jdbcUsername;
    return this;
  }

  public String getJdbcPassword() {
    return jdbcPassword;
  }

  public AbstractEngineConfiguration setJdbcPassword(String jdbcPassword) {
    this.jdbcPassword = jdbcPassword;
    return this;
  }

  public int getJdbcMaxActiveConnections() {
    return jdbcMaxActiveConnections;
  }

  public AbstractEngineConfiguration setJdbcMaxActiveConnections(int jdbcMaxActiveConnections) {
    this.jdbcMaxActiveConnections = jdbcMaxActiveConnections;
    return this;
  }

  public int getJdbcMaxIdleConnections() {
    return jdbcMaxIdleConnections;
  }

  public AbstractEngineConfiguration setJdbcMaxIdleConnections(int jdbcMaxIdleConnections) {
    this.jdbcMaxIdleConnections = jdbcMaxIdleConnections;
    return this;
  }

  public int getJdbcMaxCheckoutTime() {
    return jdbcMaxCheckoutTime;
  }

  public AbstractEngineConfiguration setJdbcMaxCheckoutTime(int jdbcMaxCheckoutTime) {
    this.jdbcMaxCheckoutTime = jdbcMaxCheckoutTime;
    return this;
  }

  public int getJdbcMaxWaitTime() {
    return jdbcMaxWaitTime;
  }

  public AbstractEngineConfiguration setJdbcMaxWaitTime(int jdbcMaxWaitTime) {
    this.jdbcMaxWaitTime = jdbcMaxWaitTime;
    return this;
  }

  public boolean isJdbcPingEnabled() {
    return jdbcPingEnabled;
  }

  public AbstractEngineConfiguration setJdbcPingEnabled(boolean jdbcPingEnabled) {
    this.jdbcPingEnabled = jdbcPingEnabled;
    return this;
  }

  public int getJdbcPingConnectionNotUsedFor() {
    return jdbcPingConnectionNotUsedFor;
  }

  public AbstractEngineConfiguration setJdbcPingConnectionNotUsedFor(int jdbcPingConnectionNotUsedFor) {
    this.jdbcPingConnectionNotUsedFor = jdbcPingConnectionNotUsedFor;
    return this;
  }

  public int getJdbcDefaultTransactionIsolationLevel() {
    return jdbcDefaultTransactionIsolationLevel;
  }

  public AbstractEngineConfiguration setJdbcDefaultTransactionIsolationLevel(int jdbcDefaultTransactionIsolationLevel) {
    this.jdbcDefaultTransactionIsolationLevel = jdbcDefaultTransactionIsolationLevel;
    return this;
  }

  public String getJdbcPingQuery() {
    return jdbcPingQuery;
  }

  public AbstractEngineConfiguration setJdbcPingQuery(String jdbcPingQuery) {
    this.jdbcPingQuery = jdbcPingQuery;
    return this;
  }

  public String getDataSourceJndiName() {
    return dataSourceJndiName;
  }

  public AbstractEngineConfiguration setDataSourceJndiName(String dataSourceJndiName) {
    this.dataSourceJndiName = dataSourceJndiName;
    return this;
  }

  public CommandConfig getSchemaCommandConfig() {
    return schemaCommandConfig;
  }

  public AbstractEngineConfiguration setSchemaCommandConfig(CommandConfig schemaCommandConfig) {
    this.schemaCommandConfig = schemaCommandConfig;
    return this;
  }

  public boolean isTransactionsExternallyManaged() {
    return transactionsExternallyManaged;
  }

  public AbstractEngineConfiguration setTransactionsExternallyManaged(boolean transactionsExternallyManaged) {
    this.transactionsExternallyManaged = transactionsExternallyManaged;
    return this;
  }
  
  public Map<Object, Object> getBeans() {
    return beans;
  }

  public AbstractEngineConfiguration setBeans(Map<Object, Object> beans) {
    this.beans = beans;
    return this;
  }

  public IdGenerator getIdGenerator() {
    return idGenerator;
  }

  public AbstractEngineConfiguration setIdGenerator(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
    return this;
  }

  public String getXmlEncoding() {
    return xmlEncoding;
  }

  public AbstractEngineConfiguration setXmlEncoding(String xmlEncoding) {
    this.xmlEncoding = xmlEncoding;
    return this;
  }

  public CommandConfig getDefaultCommandConfig() {
    return defaultCommandConfig;
  }

  public AbstractEngineConfiguration setDefaultCommandConfig(CommandConfig defaultCommandConfig) {
    this.defaultCommandConfig = defaultCommandConfig;
    return this;
  }
  
  public SqlSessionFactory getSqlSessionFactory() {
    return sqlSessionFactory;
  }

  public AbstractEngineConfiguration setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    return this;
  }

  public TransactionFactory getTransactionFactory() {
    return transactionFactory;
  }

  public AbstractEngineConfiguration setTransactionFactory(TransactionFactory transactionFactory) {
    this.transactionFactory = transactionFactory;
    return this;
  }

  public Set<Class<?>> getCustomMybatisMappers() {
    return customMybatisMappers;
  }

  public AbstractEngineConfiguration setCustomMybatisMappers(Set<Class<?>> customMybatisMappers) {
    this.customMybatisMappers = customMybatisMappers;
    return this;
  }

  public Set<String> getCustomMybatisXMLMappers() {
    return customMybatisXMLMappers;
  }

  public AbstractEngineConfiguration setCustomMybatisXMLMappers(Set<String> customMybatisXMLMappers) {
    this.customMybatisXMLMappers = customMybatisXMLMappers;
    return this;
  }

  public List<SessionFactory> getCustomSessionFactories() {
    return customSessionFactories;
  }

  public AbstractEngineConfiguration setCustomSessionFactories(List<SessionFactory> customSessionFactories) {
    this.customSessionFactories = customSessionFactories;
    return this;
  }

  public boolean isUsingRelationalDatabase() {
    return usingRelationalDatabase;
  }

  public AbstractEngineConfiguration setUsingRelationalDatabase(boolean usingRelationalDatabase) {
    this.usingRelationalDatabase = usingRelationalDatabase;
    return this;
  }

  public String getDatabaseTablePrefix() {
    return databaseTablePrefix;
  }

  public AbstractEngineConfiguration setDatabaseTablePrefix(String databaseTablePrefix) {
    this.databaseTablePrefix = databaseTablePrefix;
    return this;
  }

  public String getDatabaseWildcardEscapeCharacter() {
    return databaseWildcardEscapeCharacter;
  }

  public AbstractEngineConfiguration setDatabaseWildcardEscapeCharacter(String databaseWildcardEscapeCharacter) {
    this.databaseWildcardEscapeCharacter = databaseWildcardEscapeCharacter;
    return this;
  }

  public String getDatabaseCatalog() {
    return databaseCatalog;
  }

  public AbstractEngineConfiguration setDatabaseCatalog(String databaseCatalog) {
    this.databaseCatalog = databaseCatalog;
    return this;
  }

  public String getDatabaseSchema() {
    return databaseSchema;
  }

  public AbstractEngineConfiguration setDatabaseSchema(String databaseSchema) {
    this.databaseSchema = databaseSchema;
    return this;
  }

  public boolean isTablePrefixIsSchema() {
    return tablePrefixIsSchema;
  }

  public AbstractEngineConfiguration setTablePrefixIsSchema(boolean tablePrefixIsSchema) {
    this.tablePrefixIsSchema = tablePrefixIsSchema;
    return this;
  }

  public Map<Class<?>, SessionFactory> getSessionFactories() {
    return sessionFactories;
  }

  public AbstractEngineConfiguration setSessionFactories(Map<Class<?>, SessionFactory> sessionFactories) {
    this.sessionFactories = sessionFactories;
    return this;
  }
  
  public String getDatabaseSchemaUpdate() {
    return databaseSchemaUpdate;
  }
  
  public AbstractEngineConfiguration setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
    this.databaseSchemaUpdate = databaseSchemaUpdate;
    return this;
  }

  public boolean isEnableEventDispatcher() {
    return enableEventDispatcher;
  }

  public AbstractEngineConfiguration setEnableEventDispatcher(boolean enableEventDispatcher) {
    this.enableEventDispatcher = enableEventDispatcher;
    return this;
  }

  public FlowableEventDispatcher getEventDispatcher() {
    return eventDispatcher;
  }

  public AbstractEngineConfiguration setEventDispatcher(FlowableEventDispatcher eventDispatcher) {
    this.eventDispatcher = eventDispatcher;
    return this;
  }

  public List<FlowableEventListener> getEventListeners() {
    return eventListeners;
  }

  public AbstractEngineConfiguration setEventListeners(List<FlowableEventListener> eventListeners) {
    this.eventListeners = eventListeners;
    return this;
  }

  public Map<String, List<FlowableEventListener>> getTypedEventListeners() {
    return typedEventListeners;
  }

  public AbstractEngineConfiguration setTypedEventListeners(Map<String, List<FlowableEventListener>> typedEventListeners) {
    this.typedEventListeners = typedEventListeners;
    return this;
  }

  public Clock getClock() {
    return clock;
  }

  public AbstractEngineConfiguration setClock(Clock clock) {
    this.clock = clock;
    return this;
  }
}
