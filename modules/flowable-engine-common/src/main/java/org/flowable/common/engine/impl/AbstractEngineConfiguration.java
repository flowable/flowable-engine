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
package org.flowable.common.engine.impl;

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
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.cfg.CommandExecutorImpl;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.cfg.TransactionContextFactory;
import org.flowable.common.engine.impl.cfg.standalone.StandaloneMybatisTransactionContextFactory;
import org.flowable.common.engine.impl.db.CommonDbSchemaManager;
import org.flowable.common.engine.impl.db.DbSchemaManager;
import org.flowable.common.engine.impl.db.DbSqlSessionFactory;
import org.flowable.common.engine.impl.db.LogSqlExecutionTimePlugin;
import org.flowable.common.engine.impl.db.MybatisTypeAliasConfigurator;
import org.flowable.common.engine.impl.db.MybatisTypeHandlerConfigurator;
import org.flowable.common.engine.impl.event.EventDispatchAction;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContextFactory;
import org.flowable.common.engine.impl.interceptor.CommandContextInterceptor;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.interceptor.DefaultCommandInvoker;
import org.flowable.common.engine.impl.interceptor.LogInterceptor;
import org.flowable.common.engine.impl.interceptor.SessionFactory;
import org.flowable.common.engine.impl.interceptor.TransactionContextInterceptor;
import org.flowable.common.engine.impl.persistence.GenericManagerFactory;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.common.engine.impl.persistence.cache.EntityCacheImpl;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.common.engine.impl.util.DefaultClockImpl;
import org.flowable.common.engine.impl.util.IoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEngineConfiguration {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractEngineConfiguration.class);

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
    protected String jdbcUrl = "jdbc:h2:tcp://localhost/~/flowable";
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
    protected DbSchemaManager commonDbSchemaManager;
    protected DbSchemaManager dbSchemaManager;

    protected String databaseSchemaUpdate = DB_SCHEMA_UPDATE_FALSE;

    protected String xmlEncoding = "UTF-8";

    // COMMAND EXECUTORS ///////////////////////////////////////////////

    protected CommandExecutor commandExecutor;
    protected Collection<? extends CommandInterceptor> defaultCommandInterceptors;
    protected CommandConfig defaultCommandConfig;
    protected CommandConfig schemaCommandConfig;
    protected CommandContextFactory commandContextFactory;
    protected CommandInterceptor commandInvoker;

    protected List<CommandInterceptor> customPreCommandInterceptors;
    protected List<CommandInterceptor> customPostCommandInterceptors;
    protected List<CommandInterceptor> commandInterceptors;

    protected Map<String, AbstractEngineConfiguration> engineConfigurations = new HashMap<>();
    protected Map<String, AbstractServiceConfiguration> serviceConfigurations = new HashMap<>();

    protected ClassLoader classLoader;
    /**
     * Either use Class.forName or ClassLoader.loadClass for class loading. See http://forums.activiti.org/content/reflectutilloadclass-and-custom- classloader
     */
    protected boolean useClassForNameClassLoading = true;

    // MYBATIS SQL SESSION FACTORY /////////////////////////////////////

    protected boolean isDbHistoryUsed = true;
    protected DbSqlSessionFactory dbSqlSessionFactory;
    protected SqlSessionFactory sqlSessionFactory;
    protected TransactionFactory transactionFactory;
    protected TransactionContextFactory transactionContextFactory;

    /**
     * If set to true, enables bulk insert (grouping sql inserts together). Default true.
     * For some databases (eg DB2+z/OS) needs to be set to false.
     */
    protected boolean isBulkInsertEnabled = true;

    /**
     * Some databases have a limit of how many parameters one sql insert can have (eg SQL Server, 2000 params (!= insert statements) ). Tweak this parameter in case of exceptions indicating too much
     * is being put into one bulk insert, or make it higher if your database can cope with it and there are inserts with a huge amount of data.
     * <p>
     * By default: 100 (75 for mssql server as it has a hard limit of 2000 parameters in a statement)
     */
    protected int maxNrOfStatementsInBulkInsert = 100;

    public int DEFAULT_MAX_NR_OF_STATEMENTS_BULK_INSERT_SQL_SERVER = 60; // currently Execution has most params (31). 2000 / 31 = 64.

    protected Set<Class<?>> customMybatisMappers;
    protected Set<String> customMybatisXMLMappers;

    protected Set<String> dependentEngineMyBatisXmlMappers;
    protected List<MybatisTypeAliasConfigurator> dependentEngineMybatisTypeAliasConfigs;
    protected List<MybatisTypeHandlerConfigurator> dependentEngineMybatisTypeHandlerConfigs;

    // SESSION FACTORIES ///////////////////////////////////////////////
    protected List<SessionFactory> customSessionFactories;
    protected Map<Class<?>, SessionFactory> sessionFactories;

    protected boolean enableEventDispatcher = true;
    protected FlowableEventDispatcher eventDispatcher;
    protected List<FlowableEventListener> eventListeners;
    protected Map<String, List<FlowableEventListener>> typedEventListeners;
    protected List<EventDispatchAction> additionalEventDispatchActions;

    protected boolean transactionsExternallyManaged;

    /**
     * Flag that can be set to configure or not a relational database is used. This is useful for custom implementations that do not use relational databases
     * at all.
     *
     * If true (default), the {@link AbstractEngineConfiguration#getDatabaseSchemaUpdate()} value will be used to determine what needs to happen wrt the database schema.
     *
     * If false, no validation or schema creation will be done. That means that the database schema must have been created 'manually' before but the engine does not validate whether the schema is
     * correct. The {@link AbstractEngineConfiguration#getDatabaseSchemaUpdate()} value will not be used.
     */
    protected boolean usingRelationalDatabase = true;

    /**
     * Allows configuring a database table prefix which is used for all runtime operations of the process engine. For example, if you specify a prefix named 'PRE1.', Flowable will query for executions
     * in a table named 'PRE1.ACT_RU_EXECUTION_'.
     *
     * <p />
     * <strong>NOTE: the prefix is not respected by automatic database schema management. If you use {@link AbstractEngineConfiguration#DB_SCHEMA_UPDATE_CREATE_DROP} or
     * {@link AbstractEngineConfiguration#DB_SCHEMA_UPDATE_TRUE}, Flowable will create the database tables using the default names, regardless of the prefix configured here.</strong>
     */
    protected String databaseTablePrefix = "";

    /**
     * Escape character for doing wildcard searches.
     *
     * This will be added at then end of queries that include for example a LIKE clause. For example: SELECT * FROM table WHERE column LIKE '%\%%' ESCAPE '\';
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
     * Set to true in case the defined databaseTablePrefix is a schema-name, instead of an actual table name prefix. This is relevant for checking if Flowable-tables exist, the databaseTablePrefix
     * will not be used here - since the schema is taken into account already, adding a prefix for the table-check will result in wrong table-names.
     */
    protected boolean tablePrefixIsSchema;

    /**
     * Enables the MyBatis plugin that logs the execution time of sql statements.
     */
    protected boolean enableLogSqlExecutionTime;

    protected Properties databaseTypeMappings = getDefaultDatabaseTypeMappings();

    protected List<EngineDeployer> customPreDeployers;
    protected List<EngineDeployer> customPostDeployers;
    protected List<EngineDeployer> deployers;

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

    // Variables

    public static final int DEFAULT_GENERIC_MAX_LENGTH_STRING = 4000;
    public static final int DEFAULT_ORACLE_MAX_LENGTH_STRING = 2000;

    /**
     * Define a max length for storing String variable types in the database. Mainly used for the Oracle NVARCHAR2 limit of 2000 characters
     */
    protected int maxLengthStringVariableType = -1;

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

                LOGGER.debug("initializing datasource to db: {}", jdbcUrl);

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Configuring Datasource with following properties (omitted password for security)");
                    LOGGER.info("datasource driver : {}", jdbcDriver);
                    LOGGER.info("datasource url : {}", jdbcUrl);
                    LOGGER.info("datasource user name : {}", jdbcUsername);
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
                if (jdbcPingEnabled) {
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
            LOGGER.debug("database product name: '{}'", databaseProductName);
            databaseType = databaseTypeMappings.getProperty(databaseProductName);
            if (databaseType == null) {
                throw new FlowableException("couldn't deduct database type from database product name '" + databaseProductName + "'");
            }
            LOGGER.debug("using database type: {}", databaseType);

        } catch (SQLException e) {
            LOGGER.error("Exception while initializing Database connection", e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.error("Exception while closing the Database connection", e);
            }
        }

        // Special care for MSSQL, as it has a hard limit of 2000 params per statement (incl bulk statement).
        // Especially with executions, with 100 as default, this limit is passed.
        if (DATABASE_TYPE_MSSQL.equals(databaseType)) {
            maxNrOfStatementsInBulkInsert = DEFAULT_MAX_NR_OF_STATEMENTS_BULK_INSERT_SQL_SERVER;
        }
    }

    public void initDbSchemaManager() {
        if (this.commonDbSchemaManager == null) {
            this.commonDbSchemaManager = new CommonDbSchemaManager();
        }
    }

    // session factories ////////////////////////////////////////////////////////

    public void addSessionFactory(SessionFactory sessionFactory) {
        sessionFactories.put(sessionFactory.getSessionType(), sessionFactory);
    }

    public void initCommandContextFactory() {
        if (commandContextFactory == null) {
            commandContextFactory = new CommandContextFactory();
        }
    }

    public void initTransactionContextFactory() {
        if (transactionContextFactory == null) {
            transactionContextFactory = new StandaloneMybatisTransactionContextFactory();
        }
    }

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
            schemaCommandConfig = new CommandConfig();
        }
    }

    public void initCommandInvoker() {
        if (commandInvoker == null) {
            commandInvoker = new DefaultCommandInvoker();
        }
    }

    public void initCommandInterceptors() {
        if (commandInterceptors == null) {
            commandInterceptors = new ArrayList<>();
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
        if (defaultCommandInterceptors == null) {
            List<CommandInterceptor> interceptors = new ArrayList<>();
            interceptors.add(new LogInterceptor());

            CommandInterceptor transactionInterceptor = createTransactionInterceptor();
            if (transactionInterceptor != null) {
                interceptors.add(transactionInterceptor);
            }

            if (commandContextFactory != null) {
                String engineCfgKey = getEngineCfgKey();
                CommandContextInterceptor commandContextInterceptor = new CommandContextInterceptor(commandContextFactory);
                engineConfigurations.put(engineCfgKey, this);
                commandContextInterceptor.setEngineConfigurations(engineConfigurations);
                commandContextInterceptor.setServiceConfigurations(serviceConfigurations);
                commandContextInterceptor.setCurrentEngineConfigurationKey(engineCfgKey);
                interceptors.add(commandContextInterceptor);
            }

            if (transactionContextFactory != null) {
                interceptors.add(new TransactionContextInterceptor(transactionContextFactory));
            }

            List<CommandInterceptor> additionalCommandInterceptors = getAdditionalDefaultCommandInterceptors();
            if (additionalCommandInterceptors != null) {
                interceptors.addAll(additionalCommandInterceptors);
            }

            defaultCommandInterceptors = interceptors;
        }
        return defaultCommandInterceptors;
    }

    public abstract String getEngineCfgKey();

    public List<CommandInterceptor> getAdditionalDefaultCommandInterceptors() {
        return null;
    }

    public void initCommandExecutor() {
        if (commandExecutor == null) {
            CommandInterceptor first = initInterceptorChain(commandInterceptors);
            commandExecutor = new CommandExecutorImpl(getDefaultCommandConfig(), first);
        }
    }

    public CommandInterceptor initInterceptorChain(List<CommandInterceptor> chain) {
        if (chain == null || chain.isEmpty()) {
            throw new FlowableException("invalid command interceptor chain configuration: " + chain);
        }
        for (int i = 0; i < chain.size() - 1; i++) {
            chain.get(i).setNext(chain.get(i + 1));
        }
        return chain.get(0);
    }

    public abstract CommandInterceptor createTransactionInterceptor();


    public void initBeans() {
        if (beans == null) {
            beans = new HashMap<>();
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

    // services
    // /////////////////////////////////////////////////////////////////

    protected void initService(Object service) {
        if (service instanceof CommonEngineServiceImpl) {
            ((CommonEngineServiceImpl) service).setCommandExecutor(commandExecutor);
        }
    }

    // myBatis SqlSessionFactory
    // ////////////////////////////////////////////////

    public void initSessionFactories() {
        if (sessionFactories == null) {
            sessionFactories = new HashMap<>();

            if (usingRelationalDatabase) {
                initDbSqlSessionFactory();
            }

            addSessionFactory(new GenericManagerFactory(EntityCache.class, EntityCacheImpl.class));
            commandContextFactory.setSessionFactories(sessionFactories);
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
        dbSqlSessionFactory.setDbHistoryUsed(isDbHistoryUsed);
        dbSqlSessionFactory.setDatabaseTablePrefix(databaseTablePrefix);
        dbSqlSessionFactory.setTablePrefixIsSchema(tablePrefixIsSchema);
        dbSqlSessionFactory.setDatabaseCatalog(databaseCatalog);
        dbSqlSessionFactory.setDatabaseSchema(databaseSchema);
        dbSqlSessionFactory.setMaxNrOfStatementsInBulkInsert(maxNrOfStatementsInBulkInsert);

        initDbSqlSessionFactoryEntitySettings();

        addSessionFactory(dbSqlSessionFactory);
    }

    public DbSqlSessionFactory createDbSqlSessionFactory() {
        return new DbSqlSessionFactory();
    }

    protected abstract void initDbSqlSessionFactoryEntitySettings();

    protected void defaultInitDbSqlSessionFactoryEntitySettings(List<Class<? extends Entity>> insertOrder, List<Class<? extends Entity>> deleteOrder) {
        for (Class<? extends Entity> clazz : insertOrder) {
            dbSqlSessionFactory.getInsertionOrder().add(clazz);

            if (isBulkInsertEnabled) {
                dbSqlSessionFactory.getBulkInserteableEntityClasses().add(clazz);
            }
        }

        for (Class<? extends Entity> clazz : deleteOrder) {
            dbSqlSessionFactory.getDeletionOrder().add(clazz);
        }
    }

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
                    wildcardEscapeClause = " escape '" + databaseWildcardEscapeCharacter + "'";
                }
                properties.put("wildcardEscapeClause", wildcardEscapeClause);

                // set default properties
                properties.put("limitBefore", "");
                properties.put("limitAfter", "");
                properties.put("limitBetween", "");
                properties.put("limitOuterJoinBetween", "");
                properties.put("limitBeforeNativeQuery", "");
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

    public String pathToEngineDbProperties() {
        return "org/flowable/common/db/properties/" + databaseType + ".properties";
    }

    public Configuration initMybatisConfiguration(Environment environment, Reader reader, Properties properties) {
        XMLConfigBuilder parser = new XMLConfigBuilder(reader, "", properties);
        Configuration configuration = parser.getConfiguration();

        if (databaseType != null) {
            configuration.setDatabaseId(databaseType);
        }

        configuration.setEnvironment(environment);

        initCustomMybatisMappers(configuration);
        initMybatisTypeHandlers(configuration);

        if (isEnableLogSqlExecutionTime()) {
            initMyBatisLogSqlExecutionTimePlugin(configuration);
        }

        configuration = parseMybatisConfiguration(parser);
        return configuration;
    }

    public void initCustomMybatisMappers(Configuration configuration) {
        if (getCustomMybatisMappers() != null) {
            for (Class<?> clazz : getCustomMybatisMappers()) {
                configuration.addMapper(clazz);
            }
        }
    }

    public void initMybatisTypeHandlers(Configuration configuration) {
        // To be extended
    }

    public void initMyBatisLogSqlExecutionTimePlugin(Configuration configuration) {
        configuration.addInterceptor(new LogSqlExecutionTimePlugin());
    }

    public Configuration parseMybatisConfiguration(XMLConfigBuilder parser) {
        Configuration configuration = parser.parse();

        if (dependentEngineMybatisTypeAliasConfigs != null) {
            for (MybatisTypeAliasConfigurator typeAliasConfig : dependentEngineMybatisTypeAliasConfigs) {
                typeAliasConfig.configure(configuration.getTypeAliasRegistry());
            }
        }
        if (dependentEngineMybatisTypeHandlerConfigs != null) {
            for (MybatisTypeHandlerConfigurator typeHandlerConfig : dependentEngineMybatisTypeHandlerConfigs) {
                typeHandlerConfig.configure(configuration.getTypeHandlerRegistry());
            }
        }

        parseDependentEngineMybatisXMLMappers(configuration);
        parseCustomMybatisXMLMappers(configuration);
        return configuration;
    }

    public void parseCustomMybatisXMLMappers(Configuration configuration) {
        if (getCustomMybatisXMLMappers() != null) {
            for (String resource : getCustomMybatisXMLMappers()) {
                parseMybatisXmlMapping(configuration, resource);
            }
        }
    }

    public void parseDependentEngineMybatisXMLMappers(Configuration configuration) {
        if (getDependentEngineMyBatisXmlMappers() != null) {
            for (String resource : getDependentEngineMyBatisXmlMappers()) {
                parseMybatisXmlMapping(configuration, resource);
            }
        }
    }

    protected void parseMybatisXmlMapping(Configuration configuration, String resource) {
        // see XMLConfigBuilder.mapperElement()
        XMLMapperBuilder mapperParser = new XMLMapperBuilder(getResourceAsStream(resource), configuration, resource, configuration.getSqlFragments());
        mapperParser.parse();
    }

    protected InputStream getResourceAsStream(String resource) {
        ClassLoader classLoader = getClassLoader();
        if (classLoader != null) {
            return getClassLoader().getResourceAsStream(resource);
        } else {
            return this.getClass().getClassLoader().getResourceAsStream(resource);
        }
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

    public DbSchemaManager getDbSchemaManager() {
        return dbSchemaManager;
    }

    public AbstractEngineConfiguration setDbSchemaManager(DbSchemaManager dbSchemaManager) {
        this.dbSchemaManager = dbSchemaManager;
        return this;
    }

    public DbSchemaManager getCommonDbSchemaManager() {
        return commonDbSchemaManager;
    }

    public AbstractEngineConfiguration setCommonDbSchemaManager(DbSchemaManager commonDbSchemaManager) {
        this.commonDbSchemaManager = commonDbSchemaManager;
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

    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    public AbstractEngineConfiguration setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        return this;
    }

    public CommandContextFactory getCommandContextFactory() {
        return commandContextFactory;
    }

    public AbstractEngineConfiguration setCommandContextFactory(CommandContextFactory commandContextFactory) {
        this.commandContextFactory = commandContextFactory;
        return this;
    }

    public CommandInterceptor getCommandInvoker() {
        return commandInvoker;
    }

    public AbstractEngineConfiguration setCommandInvoker(CommandInterceptor commandInvoker) {
        this.commandInvoker = commandInvoker;
        return this;
    }

    public List<CommandInterceptor> getCustomPreCommandInterceptors() {
        return customPreCommandInterceptors;
    }

    public AbstractEngineConfiguration setCustomPreCommandInterceptors(List<CommandInterceptor> customPreCommandInterceptors) {
        this.customPreCommandInterceptors = customPreCommandInterceptors;
        return this;
    }

    public List<CommandInterceptor> getCustomPostCommandInterceptors() {
        return customPostCommandInterceptors;
    }

    public AbstractEngineConfiguration setCustomPostCommandInterceptors(List<CommandInterceptor> customPostCommandInterceptors) {
        this.customPostCommandInterceptors = customPostCommandInterceptors;
        return this;
    }

    public List<CommandInterceptor> getCommandInterceptors() {
        return commandInterceptors;
    }

    public AbstractEngineConfiguration setCommandInterceptors(List<CommandInterceptor> commandInterceptors) {
        this.commandInterceptors = commandInterceptors;
        return this;
    }

    public Map<String, AbstractEngineConfiguration> getEngineConfigurations() {
        return engineConfigurations;
    }

    public AbstractEngineConfiguration setEngineConfigurations(Map<String, AbstractEngineConfiguration> engineConfigurations) {
        this.engineConfigurations = engineConfigurations;
        return this;
    }

    public void addEngineConfiguration(String key, AbstractEngineConfiguration engineConfiguration) {
        if (engineConfigurations == null) {
            engineConfigurations = new HashMap<>();
        }
        engineConfigurations.put(key, engineConfiguration);
    }

    public Map<String, AbstractServiceConfiguration> getServiceConfigurations() {
        return serviceConfigurations;
    }

    public AbstractEngineConfiguration setServiceConfigurations(Map<String, AbstractServiceConfiguration> serviceConfigurations) {
        this.serviceConfigurations = serviceConfigurations;
        return this;
    }

    public void addServiceConfiguration(String key, AbstractServiceConfiguration serviceConfiguration) {
        if (serviceConfigurations == null) {
            serviceConfigurations = new HashMap<>();
        }
        serviceConfigurations.put(key, serviceConfiguration);
    }

    public void setDefaultCommandInterceptors(Collection<? extends CommandInterceptor> defaultCommandInterceptors) {
        this.defaultCommandInterceptors = defaultCommandInterceptors;
    }

    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    public AbstractEngineConfiguration setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
        return this;
    }

    public boolean isDbHistoryUsed() {
        return isDbHistoryUsed;
    }

    public AbstractEngineConfiguration setDbHistoryUsed(boolean isDbHistoryUsed) {
        this.isDbHistoryUsed = isDbHistoryUsed;
        return this;
    }

    public DbSqlSessionFactory getDbSqlSessionFactory() {
        return dbSqlSessionFactory;
    }

    public AbstractEngineConfiguration setDbSqlSessionFactory(DbSqlSessionFactory dbSqlSessionFactory) {
        this.dbSqlSessionFactory = dbSqlSessionFactory;
        return this;
    }

    public TransactionFactory getTransactionFactory() {
        return transactionFactory;
    }

    public AbstractEngineConfiguration setTransactionFactory(TransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
        return this;
    }

    public TransactionContextFactory getTransactionContextFactory() {
        return transactionContextFactory;
    }

    public AbstractEngineConfiguration setTransactionContextFactory(TransactionContextFactory transactionContextFactory) {
        this.transactionContextFactory = transactionContextFactory;
        return this;
    }

    public int getMaxNrOfStatementsInBulkInsert() {
        return maxNrOfStatementsInBulkInsert;
    }

    public AbstractEngineConfiguration setMaxNrOfStatementsInBulkInsert(int maxNrOfStatementsInBulkInsert) {
        this.maxNrOfStatementsInBulkInsert = maxNrOfStatementsInBulkInsert;
        return this;
    }

    public boolean isBulkInsertEnabled() {
        return isBulkInsertEnabled;
    }

    public AbstractEngineConfiguration setBulkInsertEnabled(boolean isBulkInsertEnabled) {
        this.isBulkInsertEnabled = isBulkInsertEnabled;
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

    public Set<String> getDependentEngineMyBatisXmlMappers() {
        return dependentEngineMyBatisXmlMappers;
    }

    public AbstractEngineConfiguration setDependentEngineMyBatisXmlMappers(Set<String> dependentEngineMyBatisXmlMappers) {
        this.dependentEngineMyBatisXmlMappers = dependentEngineMyBatisXmlMappers;
        return this;
    }

    public List<MybatisTypeAliasConfigurator> getDependentEngineMybatisTypeAliasConfigs() {
        return dependentEngineMybatisTypeAliasConfigs;
    }

    public AbstractEngineConfiguration setDependentEngineMybatisTypeAliasConfigs(List<MybatisTypeAliasConfigurator> dependentEngineMybatisTypeAliasConfigs) {
        this.dependentEngineMybatisTypeAliasConfigs = dependentEngineMybatisTypeAliasConfigs;
        return this;
    }

    public List<MybatisTypeHandlerConfigurator> getDependentEngineMybatisTypeHandlerConfigs() {
        return dependentEngineMybatisTypeHandlerConfigs;
    }

    public AbstractEngineConfiguration setDependentEngineMybatisTypeHandlerConfigs(List<MybatisTypeHandlerConfigurator> dependentEngineMybatisTypeHandlerConfigs) {
        this.dependentEngineMybatisTypeHandlerConfigs = dependentEngineMybatisTypeHandlerConfigs;
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

    public boolean isEnableLogSqlExecutionTime() {
        return enableLogSqlExecutionTime;
    }

    public void setEnableLogSqlExecutionTime(boolean enableLogSqlExecutionTime) {
        this.enableLogSqlExecutionTime = enableLogSqlExecutionTime;
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

    public List<EventDispatchAction> getAdditionalEventDispatchActions() {
        return additionalEventDispatchActions;
    }

    public AbstractEngineConfiguration setAdditionalEventDispatchActions(List<EventDispatchAction> additionalEventDispatchActions) {
        this.additionalEventDispatchActions = additionalEventDispatchActions;
        return this;
    }

    public Clock getClock() {
        return clock;
    }

    public AbstractEngineConfiguration setClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public int getMaxLengthString() {
        if (maxLengthStringVariableType == -1) {
            if ("oracle".equalsIgnoreCase(databaseType)) {
                return DEFAULT_ORACLE_MAX_LENGTH_STRING;
            } else {
                return DEFAULT_GENERIC_MAX_LENGTH_STRING;
            }
        } else {
            return maxLengthStringVariableType;
        }
    }

    public int getMaxLengthStringVariableType() {
        return maxLengthStringVariableType;
    }

    public AbstractEngineConfiguration setMaxLengthStringVariableType(int maxLengthStringVariableType) {
        this.maxLengthStringVariableType = maxLengthStringVariableType;
        return this;
    }

    public List<EngineDeployer> getDeployers() {
        return deployers;
    }

    public AbstractEngineConfiguration setDeployers(List<EngineDeployer> deployers) {
        this.deployers = deployers;
        return this;
    }

    public List<EngineDeployer> getCustomPreDeployers() {
        return customPreDeployers;
    }

    public AbstractEngineConfiguration setCustomPreDeployers(List<EngineDeployer> customPreDeployers) {
        this.customPreDeployers = customPreDeployers;
        return this;
    }

    public List<EngineDeployer> getCustomPostDeployers() {
        return customPostDeployers;
    }

    public AbstractEngineConfiguration setCustomPostDeployers(List<EngineDeployer> customPostDeployers) {
        this.customPostDeployers = customPostDeployers;
        return this;
    }

}
