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
package org.flowable.dmn.engine;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.flowable.dmn.api.DmnManagementService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.engine.impl.DmnEngineImpl;
import org.flowable.dmn.engine.impl.DmnManagementServiceImpl;
import org.flowable.dmn.engine.impl.DmnRepositoryServiceImpl;
import org.flowable.dmn.engine.impl.DmnRuleServiceImpl;
import org.flowable.dmn.engine.impl.RuleEngineExecutorImpl;
import org.flowable.dmn.engine.impl.ServiceImpl;
import org.flowable.dmn.engine.impl.cfg.CommandExecutorImpl;
import org.flowable.dmn.engine.impl.cfg.StandaloneDmnEngineConfiguration;
import org.flowable.dmn.engine.impl.cfg.StandaloneInMemDmnEngineConfiguration;
import org.flowable.dmn.engine.impl.cfg.TransactionListener;
import org.flowable.dmn.engine.impl.cfg.standalone.StandaloneMybatisTransactionContextFactory;
import org.flowable.dmn.engine.impl.db.DbSqlSessionFactory;
import org.flowable.dmn.engine.impl.deployer.CachingAndArtifactsManager;
import org.flowable.dmn.engine.impl.deployer.DmnDeployer;
import org.flowable.dmn.engine.impl.deployer.DmnDeploymentHelper;
import org.flowable.dmn.engine.impl.deployer.ParsedDeploymentBuilderFactory;
import org.flowable.dmn.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.engine.impl.interceptor.CommandContextFactory;
import org.flowable.dmn.engine.impl.interceptor.CommandContextInterceptor;
import org.flowable.dmn.engine.impl.interceptor.CommandExecutor;
import org.flowable.dmn.engine.impl.interceptor.CommandInterceptor;
import org.flowable.dmn.engine.impl.interceptor.CommandInvoker;
import org.flowable.dmn.engine.impl.interceptor.LogInterceptor;
import org.flowable.dmn.engine.impl.interceptor.TransactionContextInterceptor;
import org.flowable.dmn.engine.impl.mvel.config.DefaultCustomExpressionFunctionRegistry;
import org.flowable.dmn.engine.impl.parser.DmnParseFactory;
import org.flowable.dmn.engine.impl.persistence.deploy.DecisionTableCacheEntry;
import org.flowable.dmn.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.dmn.engine.impl.persistence.deploy.Deployer;
import org.flowable.dmn.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.dmn.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionTableEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionTableEntityManagerImpl;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntityManagerImpl;
import org.flowable.dmn.engine.impl.persistence.entity.ResourceEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.ResourceEntityManagerImpl;
import org.flowable.dmn.engine.impl.persistence.entity.TableDataManager;
import org.flowable.dmn.engine.impl.persistence.entity.TableDataManagerImpl;
import org.flowable.dmn.engine.impl.persistence.entity.data.DecisionTableDataManager;
import org.flowable.dmn.engine.impl.persistence.entity.data.DmnDeploymentDataManager;
import org.flowable.dmn.engine.impl.persistence.entity.data.ResourceDataManager;
import org.flowable.dmn.engine.impl.persistence.entity.data.impl.MybatisDecisionTableDataManager;
import org.flowable.dmn.engine.impl.persistence.entity.data.impl.MybatisDmnDeploymentDataManager;
import org.flowable.dmn.engine.impl.persistence.entity.data.impl.MybatisResourceDataManager;
import org.flowable.engine.common.AbstractEngineConfiguration;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.cfg.BeansConfigurationHelper;
import org.flowable.engine.common.impl.cfg.TransactionContextFactory;
import org.flowable.engine.common.impl.interceptor.CommandConfig;
import org.flowable.engine.common.impl.interceptor.SessionFactory;
import org.flowable.engine.common.runtime.Clock;
import org.mvel2.integration.PropertyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

public class DmnEngineConfiguration extends AbstractEngineConfiguration {

    protected static final Logger logger = LoggerFactory.getLogger(DmnEngineConfiguration.class);

    public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/flowable/dmn/db/mapping/mappings.xml";

    public static final String LIQUIBASE_CHANGELOG_PREFIX = "ACT_DMN_";

    protected String dmnEngineName = DmnEngines.NAME_DEFAULT;

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

    protected DmnManagementService dmnManagementService = new DmnManagementServiceImpl();
    protected DmnRepositoryService dmnRepositoryService = new DmnRepositoryServiceImpl();
    protected DmnRuleService ruleService = new DmnRuleServiceImpl();
    protected RuleEngineExecutor ruleEngineExecutor = new RuleEngineExecutorImpl();

    // DATA MANAGERS ///////////////////////////////////////////////////

    protected DmnDeploymentDataManager deploymentDataManager;
    protected DecisionTableDataManager decisionTableDataManager;
    protected ResourceDataManager resourceDataManager;

    // ENTITY MANAGERS /////////////////////////////////////////////////
    protected DmnDeploymentEntityManager deploymentEntityManager;
    protected DecisionTableEntityManager decisionTableEntityManager;
    protected ResourceEntityManager resourceEntityManager;
    protected TableDataManager tableDataManager;

    protected CommandContextFactory commandContextFactory;
    protected TransactionContextFactory<TransactionListener, CommandContext> transactionContextFactory;

    // SESSION FACTORIES ///////////////////////////////////////////////
    protected DbSqlSessionFactory dbSqlSessionFactory;

    // DEPLOYERS
    // ////////////////////////////////////////////////////////////////

    protected DmnDeployer dmnDeployer;
    protected DmnParseFactory dmnParseFactory;
    protected ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory;
    protected DmnDeploymentHelper dmnDeploymentHelper;
    protected CachingAndArtifactsManager cachingAndArtifactsManager;
    protected List<Deployer> customPreDeployers;
    protected List<Deployer> customPostDeployers;
    protected List<Deployer> deployers;
    protected DeploymentManager deploymentManager;

    protected int decisionCacheLimit = -1; // By default, no limit
    protected DeploymentCache<DecisionTableCacheEntry> decisionCache;

    // CUSTOM EXPRESSION FUNCTIONS
    // ////////////////////////////////////////////////////////////////
    protected CustomExpressionFunctionRegistry customExpressionFunctionRegistry;
    protected CustomExpressionFunctionRegistry postCustomExpressionFunctionRegistry;
    protected Map<String, Method> customExpressionFunctions = new HashMap<String, Method>();
    protected Map<Class<?>, PropertyHandler> customPropertyHandlers = new HashMap<Class<?>, PropertyHandler>();

    /**
     * Set this to true if you want to have extra checks on the BPMN xml that is parsed. See http://www.jorambarrez.be/blog/2013/02/19/uploading-a-funny-xml -can-bring-down-your-server/
     * 
     * Unfortunately, this feature is not available on some platforms (JDK 6, JBoss), hence the reason why it is disabled by default. If your platform allows the use of StaxSource during XML parsing,
     * do enable it.
     */
    protected boolean enableSafeDmnXml;

    public static DmnEngineConfiguration createDmnEngineConfigurationFromResourceDefault() {
        return createDmnEngineConfigurationFromResource("flowable.dmn.cfg.xml", "dmnEngineConfiguration");
    }

    public static DmnEngineConfiguration createDmnEngineConfigurationFromResource(String resource) {
        return createDmnEngineConfigurationFromResource(resource, "dmnEngineConfiguration");
    }

    public static DmnEngineConfiguration createDmnEngineConfigurationFromResource(String resource, String beanName) {
        return (DmnEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromResource(resource, beanName);
    }

    public static DmnEngineConfiguration createDmnEngineConfigurationFromInputStream(InputStream inputStream) {
        return createDmnEngineConfigurationFromInputStream(inputStream, "dmnEngineConfiguration");
    }

    public static DmnEngineConfiguration createDmnEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
        return (DmnEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromInputStream(inputStream, beanName);
    }

    public static DmnEngineConfiguration createStandaloneDmnEngineConfiguration() {
        return new StandaloneDmnEngineConfiguration();
    }

    public static DmnEngineConfiguration createStandaloneInMemDmnEngineConfiguration() {
        return new StandaloneInMemDmnEngineConfiguration();
    }

    // buildProcessEngine
    // ///////////////////////////////////////////////////////

    public DmnEngine buildDmnEngine() {
        init();
        return new DmnEngineImpl(this);
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
        initDeployers();
        initClock();
        initCustomExpressionFunctions();
    }

    // services
    // /////////////////////////////////////////////////////////////////

    protected void initServices() {
        initService(dmnManagementService);
        initService(dmnRepositoryService);
        initService(ruleService);
    }

    protected void initService(Object service) {
        if (service instanceof ServiceImpl) {
            ((ServiceImpl) service).setCommandExecutor(commandExecutor);
        }
    }

    // Data managers
    ///////////////////////////////////////////////////////////

    public void initDataManagers() {
        if (deploymentDataManager == null) {
            deploymentDataManager = new MybatisDmnDeploymentDataManager(this);
        }
        if (decisionTableDataManager == null) {
            decisionTableDataManager = new MybatisDecisionTableDataManager(this);
        }
        if (resourceDataManager == null) {
            resourceDataManager = new MybatisResourceDataManager(this);
        }
    }

    public void initEntityManagers() {
        if (deploymentEntityManager == null) {
            deploymentEntityManager = new DmnDeploymentEntityManagerImpl(this, deploymentDataManager);
        }
        if (decisionTableEntityManager == null) {
            decisionTableEntityManager = new DecisionTableEntityManagerImpl(this, decisionTableDataManager);
        }
        if (resourceEntityManager == null) {
            resourceEntityManager = new ResourceEntityManagerImpl(this, resourceDataManager);
        }
        if (tableDataManager == null) {
            tableDataManager = new TableDataManagerImpl(this);
        }
    }

    // data model
    // ///////////////////////////////////////////////////////////////

    public void initDbSchema() {
        try {
            DatabaseConnection connection = new JdbcConnection(dataSource.getConnection());
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
            database.setDatabaseChangeLogTableName(LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogTableName());
            database.setDatabaseChangeLogLockTableName(LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogLockTableName());

            if (StringUtils.isNotEmpty(databaseSchema)) {
                database.setDefaultSchemaName(databaseSchema);
                database.setLiquibaseSchemaName(databaseSchema);
                
            } else if (StringUtils.isNotEmpty(dataSource.getConnection().getSchema())) {
                database.setDefaultSchemaName(dataSource.getConnection().getSchema());
                database.setLiquibaseSchemaName(dataSource.getConnection().getSchema());
            }

            if (StringUtils.isNotEmpty(databaseCatalog)) {
                database.setDefaultCatalogName(databaseCatalog);
                database.setLiquibaseCatalogName(databaseCatalog);
                
            } else if (StringUtils.isNotEmpty(dataSource.getConnection().getCatalog())) {
                database.setDefaultCatalogName(dataSource.getConnection().getCatalog());
                database.setLiquibaseCatalogName(dataSource.getConnection().getCatalog());
            }

            Liquibase liquibase = new Liquibase("org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml", new ClassLoaderResourceAccessor(), database);

            if (DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
                logger.debug("Dropping and creating schema DMN");
                liquibase.dropAll();
                liquibase.update("dmn");
            } else if (DB_SCHEMA_UPDATE_TRUE.equals(databaseSchemaUpdate)) {
                logger.debug("Updating schema DMN");
                liquibase.update("dmn");
            } else if (DB_SCHEMA_UPDATE_FALSE.equals(databaseSchemaUpdate)) {
                logger.debug("Validating schema DMN");
                liquibase.validate();
            }
        } catch (Exception e) {
            throw new FlowableException("Error initialising dmn data model");
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
            throw new FlowableException("invalid command interceptor chain configuration: " + chain);
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
        if (dmnParseFactory == null) {
            dmnParseFactory = new DmnParseFactory();
        }

        if (this.dmnDeployer == null) {
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
        if (decisionCache == null) {
            if (decisionCacheLimit <= 0) {
                decisionCache = new DefaultDeploymentCache<DecisionTableCacheEntry>();
            } else {
                decisionCache = new DefaultDeploymentCache<DecisionTableCacheEntry>(decisionCacheLimit);
            }
        }

        deploymentManager = new DeploymentManager(decisionCache, this);
        deploymentManager.setDeployers(deployers);
        deploymentManager.setDeploymentEntityManager(deploymentEntityManager);
        deploymentManager.setDecisionTableEntityManager(decisionTableEntityManager);
    }

    public Collection<? extends Deployer> getDefaultDeployers() {
        List<Deployer> defaultDeployers = new ArrayList<Deployer>();

        if (dmnDeployer == null) {
            dmnDeployer = new DmnDeployer();
        }

        initDmnDeployerDependencies();

        dmnDeployer.setIdGenerator(idGenerator);
        dmnDeployer.setParsedDeploymentBuilderFactory(parsedDeploymentBuilderFactory);
        dmnDeployer.setDmnDeploymentHelper(dmnDeploymentHelper);
        dmnDeployer.setCachingAndArtifactsManager(cachingAndArtifactsManager);

        defaultDeployers.add(dmnDeployer);
        return defaultDeployers;
    }

    public void initDmnDeployerDependencies() {
        if (parsedDeploymentBuilderFactory == null) {
            parsedDeploymentBuilderFactory = new ParsedDeploymentBuilderFactory();
        }
        if (parsedDeploymentBuilderFactory.getDmnParseFactory() == null) {
            parsedDeploymentBuilderFactory.setDmnParseFactory(dmnParseFactory);
        }

        if (dmnDeploymentHelper == null) {
            dmnDeploymentHelper = new DmnDeploymentHelper();
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

    // custom expression functions
    // ////////////////////////////////////////////////////////////////
    protected void initCustomExpressionFunctions() {
        if (customExpressionFunctionRegistry == null) {
            customExpressionFunctions.putAll(new DefaultCustomExpressionFunctionRegistry().getCustomExpressionMethods());
        } else {
            customExpressionFunctions.putAll(customExpressionFunctionRegistry.getCustomExpressionMethods());
        }

        if (postCustomExpressionFunctionRegistry != null) {
            customExpressionFunctions.putAll(postCustomExpressionFunctionRegistry.getCustomExpressionMethods());
        }
    }

    // myBatis SqlSessionFactory
    // ////////////////////////////////////////////////

    public String pathToEngineDbProperties() {
        return "org/flowable/dmn/db/properties/" + databaseType + ".properties";
    }

    public InputStream getMyBatisXmlConfigurationStream() {
        return getResourceAsStream(DEFAULT_MYBATIS_MAPPING_FILE);
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public String getEngineName() {
        return dmnEngineName;
    }

    public DmnEngineConfiguration setEngineName(String dmnEngineName) {
        this.dmnEngineName = dmnEngineName;
        return this;
    }

    public DmnEngineConfiguration setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
        return this;
    }

    public DmnEngineConfiguration setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public DmnEngineConfiguration setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
        return this;
    }

    public DmnEngineConfiguration setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        return this;
    }

    public DmnEngineConfiguration setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
        return this;
    }

    public DmnEngineConfiguration setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
        return this;
    }

    public DmnEngineConfiguration setJdbcMaxActiveConnections(int jdbcMaxActiveConnections) {
        this.jdbcMaxActiveConnections = jdbcMaxActiveConnections;
        return this;
    }

    public DmnEngineConfiguration setJdbcMaxIdleConnections(int jdbcMaxIdleConnections) {
        this.jdbcMaxIdleConnections = jdbcMaxIdleConnections;
        return this;
    }

    public DmnEngineConfiguration setJdbcMaxCheckoutTime(int jdbcMaxCheckoutTime) {
        this.jdbcMaxCheckoutTime = jdbcMaxCheckoutTime;
        return this;
    }

    public DmnEngineConfiguration setJdbcMaxWaitTime(int jdbcMaxWaitTime) {
        this.jdbcMaxWaitTime = jdbcMaxWaitTime;
        return this;
    }

    public DmnEngineConfiguration setJdbcPingEnabled(boolean jdbcPingEnabled) {
        this.jdbcPingEnabled = jdbcPingEnabled;
        return this;
    }

    public DmnEngineConfiguration setJdbcPingConnectionNotUsedFor(int jdbcPingConnectionNotUsedFor) {
        this.jdbcPingConnectionNotUsedFor = jdbcPingConnectionNotUsedFor;
        return this;
    }

    public DmnEngineConfiguration setJdbcDefaultTransactionIsolationLevel(int jdbcDefaultTransactionIsolationLevel) {
        this.jdbcDefaultTransactionIsolationLevel = jdbcDefaultTransactionIsolationLevel;
        return this;
    }

    public DmnEngineConfiguration setJdbcPingQuery(String jdbcPingQuery) {
        this.jdbcPingQuery = jdbcPingQuery;
        return this;
    }

    public DmnEngineConfiguration setDataSourceJndiName(String dataSourceJndiName) {
        this.dataSourceJndiName = dataSourceJndiName;
        return this;
    }

    public DmnEngineConfiguration setXmlEncoding(String xmlEncoding) {
        this.xmlEncoding = xmlEncoding;
        return this;
    }

    public DmnEngineConfiguration setBeans(Map<Object, Object> beans) {
        this.beans = beans;
        return this;
    }

    public DmnEngineConfiguration setDefaultCommandConfig(CommandConfig defaultCommandConfig) {
        this.defaultCommandConfig = defaultCommandConfig;
        return this;
    }

    public CommandInterceptor getCommandInvoker() {
        return commandInvoker;
    }

    public DmnEngineConfiguration setCommandInvoker(CommandInterceptor commandInvoker) {
        this.commandInvoker = commandInvoker;
        return this;
    }

    public List<CommandInterceptor> getCustomPreCommandInterceptors() {
        return customPreCommandInterceptors;
    }

    public DmnEngineConfiguration setCustomPreCommandInterceptors(List<CommandInterceptor> customPreCommandInterceptors) {
        this.customPreCommandInterceptors = customPreCommandInterceptors;
        return this;
    }

    public List<CommandInterceptor> getCustomPostCommandInterceptors() {
        return customPostCommandInterceptors;
    }

    public DmnEngineConfiguration setCustomPostCommandInterceptors(List<CommandInterceptor> customPostCommandInterceptors) {
        this.customPostCommandInterceptors = customPostCommandInterceptors;
        return this;
    }

    public List<CommandInterceptor> getCommandInterceptors() {
        return commandInterceptors;
    }

    public DmnEngineConfiguration setCommandInterceptors(List<CommandInterceptor> commandInterceptors) {
        this.commandInterceptors = commandInterceptors;
        return this;
    }

    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    public DmnEngineConfiguration setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        return this;
    }

    public DmnManagementService getDmnManagementService() {
        return dmnManagementService;
    }

    public DmnEngineConfiguration setDmnManagementService(DmnManagementService dmnManagementService) {
        this.dmnManagementService = dmnManagementService;
        return this;
    }

    public DmnRepositoryService getDmnRepositoryService() {
        return dmnRepositoryService;
    }

    public DmnEngineConfiguration setDmnRepositoryService(DmnRepositoryService dmnRepositoryService) {
        this.dmnRepositoryService = dmnRepositoryService;
        return this;
    }

    public DmnRuleService getDmnRuleService() {
        return ruleService;
    }

    public DmnEngineConfiguration setDmnRuleService(DmnRuleService ruleService) {
        this.ruleService = ruleService;
        return this;
    }

    public RuleEngineExecutor getRuleEngineExecutor() {
        return ruleEngineExecutor;
    }

    public DmnEngineConfiguration setRuleEngineExecutor(RuleEngineExecutor ruleEngineExecutor) {
        this.ruleEngineExecutor = ruleEngineExecutor;
        return this;
    }

    public DeploymentManager getDeploymentManager() {
        return deploymentManager;
    }

    public DmnEngineConfiguration getDmnEngineConfiguration() {
        return this;
    }

    public DmnDeployer getDmnDeployer() {
        return dmnDeployer;
    }

    public DmnEngineConfiguration setDmnDeployer(DmnDeployer dmnDeployer) {
        this.dmnDeployer = dmnDeployer;
        return this;
    }

    public DmnParseFactory getDmnParseFactory() {
        return dmnParseFactory;
    }

    public DmnEngineConfiguration setDmnParseFactory(DmnParseFactory dmnParseFactory) {
        this.dmnParseFactory = dmnParseFactory;
        return this;
    }

    public int getDecisionCacheLimit() {
        return decisionCacheLimit;
    }

    public DmnEngineConfiguration setDecisionCacheLimit(int decisionCacheLimit) {
        this.decisionCacheLimit = decisionCacheLimit;
        return this;
    }

    public DeploymentCache<DecisionTableCacheEntry> getDecisionCache() {
        return decisionCache;
    }

    public DmnEngineConfiguration setDecisionCache(DeploymentCache<DecisionTableCacheEntry> decisionCache) {
        this.decisionCache = decisionCache;
        return this;
    }

    public DmnDeploymentDataManager getDeploymentDataManager() {
        return deploymentDataManager;
    }

    public DmnEngineConfiguration setDeploymentDataManager(DmnDeploymentDataManager deploymentDataManager) {
        this.deploymentDataManager = deploymentDataManager;
        return this;
    }

    public DecisionTableDataManager getDecisionTableDataManager() {
        return decisionTableDataManager;
    }

    public DmnEngineConfiguration setDecisionTableDataManager(DecisionTableDataManager decisionTableDataManager) {
        this.decisionTableDataManager = decisionTableDataManager;
        return this;
    }

    public ResourceDataManager getResourceDataManager() {
        return resourceDataManager;
    }

    public DmnEngineConfiguration setResourceDataManager(ResourceDataManager resourceDataManager) {
        this.resourceDataManager = resourceDataManager;
        return this;
    }

    public DmnDeploymentEntityManager getDeploymentEntityManager() {
        return deploymentEntityManager;
    }

    public DmnEngineConfiguration setDeploymentEntityManager(DmnDeploymentEntityManager deploymentEntityManager) {
        this.deploymentEntityManager = deploymentEntityManager;
        return this;
    }

    public DecisionTableEntityManager getDecisionTableEntityManager() {
        return decisionTableEntityManager;
    }

    public DmnEngineConfiguration setDecisionTableEntityManager(DecisionTableEntityManager decisionTableEntityManager) {
        this.decisionTableEntityManager = decisionTableEntityManager;
        return this;
    }

    public ResourceEntityManager getResourceEntityManager() {
        return resourceEntityManager;
    }

    public DmnEngineConfiguration setResourceEntityManager(ResourceEntityManager resourceEntityManager) {
        this.resourceEntityManager = resourceEntityManager;
        return this;
    }

    public TableDataManager getTableDataManager() {
        return tableDataManager;
    }

    public DmnEngineConfiguration setTableDataManager(TableDataManager tableDataManager) {
        this.tableDataManager = tableDataManager;
        return this;
    }

    public CommandContextFactory getCommandContextFactory() {
        return commandContextFactory;
    }

    public DmnEngineConfiguration setCommandContextFactory(CommandContextFactory commandContextFactory) {
        this.commandContextFactory = commandContextFactory;
        return this;
    }

    public DmnEngineConfiguration setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
        return this;
    }

    public DmnEngineConfiguration setTransactionFactory(TransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
        return this;
    }

    public DmnEngineConfiguration setCustomMybatisMappers(Set<Class<?>> customMybatisMappers) {
        this.customMybatisMappers = customMybatisMappers;
        return this;
    }

    public DmnEngineConfiguration setCustomMybatisXMLMappers(Set<String> customMybatisXMLMappers) {
        this.customMybatisXMLMappers = customMybatisXMLMappers;
        return this;
    }

    public DmnEngineConfiguration setCustomSessionFactories(List<SessionFactory> customSessionFactories) {
        this.customSessionFactories = customSessionFactories;
        return this;
    }

    public DbSqlSessionFactory getDbSqlSessionFactory() {
        return dbSqlSessionFactory;
    }

    public DmnEngineConfiguration setDbSqlSessionFactory(DbSqlSessionFactory dbSqlSessionFactory) {
        this.dbSqlSessionFactory = dbSqlSessionFactory;
        return this;
    }

    public DmnEngineConfiguration setUsingRelationalDatabase(boolean usingRelationalDatabase) {
        this.usingRelationalDatabase = usingRelationalDatabase;
        return this;
    }

    public DmnEngineConfiguration setDatabaseTablePrefix(String databaseTablePrefix) {
        this.databaseTablePrefix = databaseTablePrefix;
        return this;
    }

    public DmnEngineConfiguration setDatabaseCatalog(String databaseCatalog) {
        this.databaseCatalog = databaseCatalog;
        return this;
    }

    public DmnEngineConfiguration setDatabaseSchema(String databaseSchema) {
        this.databaseSchema = databaseSchema;
        return this;
    }

    public DmnEngineConfiguration setTablePrefixIsSchema(boolean tablePrefixIsSchema) {
        this.tablePrefixIsSchema = tablePrefixIsSchema;
        return this;
    }

    public DmnEngineConfiguration setSessionFactories(Map<Class<?>, SessionFactory> sessionFactories) {
        this.sessionFactories = sessionFactories;
        return this;
    }

    public TransactionContextFactory<TransactionListener, CommandContext> getTransactionContextFactory() {
        return transactionContextFactory;
    }

    public DmnEngineConfiguration setTransactionContextFactory(TransactionContextFactory<TransactionListener, CommandContext> transactionContextFactory) {
        this.transactionContextFactory = transactionContextFactory;
        return this;
    }

    public boolean isEnableSafeDmnXml() {
        return enableSafeDmnXml;
    }

    public DmnEngineConfiguration setEnableSafeDmnXml(boolean enableSafeDmnXml) {
        this.enableSafeDmnXml = enableSafeDmnXml;
        return this;
    }

    public DmnEngineConfiguration setClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public CustomExpressionFunctionRegistry getCustomExpressionFunctionRegistry() {
        return customExpressionFunctionRegistry;
    }

    public DmnEngineConfiguration setCustomExpressionFunctionRegistry(CustomExpressionFunctionRegistry customExpressionFunctionRegistry) {
        this.customExpressionFunctionRegistry = customExpressionFunctionRegistry;
        return this;
    }

    public CustomExpressionFunctionRegistry getPostCustomExpressionFunctionRegistry() {
        return postCustomExpressionFunctionRegistry;
    }

    public DmnEngineConfiguration setPostCustomExpressionFunctionRegistry(CustomExpressionFunctionRegistry postCustomExpressionFunctionRegistry) {
        this.postCustomExpressionFunctionRegistry = postCustomExpressionFunctionRegistry;
        return this;
    }

    public Map<String, Method> getCustomExpressionFunctions() {
        return customExpressionFunctions;
    }

    public DmnEngineConfiguration setCustomExpressionFunctions(Map<String, Method> customExpressionFunctions) {
        this.customExpressionFunctions = customExpressionFunctions;
        return this;
    }

    public Map<Class<?>, PropertyHandler> getCustomPropertyHandlers() {
        return customPropertyHandlers;
    }

    public DmnEngineConfiguration setCustomPropertyHandlers(Map<Class<?>, PropertyHandler> customPropertyHandlers) {
        this.customPropertyHandlers = customPropertyHandlers;
        return this;
    }

    public DmnEngineConfiguration setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
        this.databaseSchemaUpdate = databaseSchemaUpdate;
        return this;
    }
}
