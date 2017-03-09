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
package org.flowable.form.engine;

import java.io.InputStream;
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
import org.flowable.editor.form.converter.FormJsonConverter;
import org.flowable.engine.common.AbstractEngineConfiguration;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.cfg.BeansConfigurationHelper;
import org.flowable.engine.common.impl.cfg.TransactionContextFactory;
import org.flowable.engine.common.impl.interceptor.CommandConfig;
import org.flowable.engine.common.impl.interceptor.SessionFactory;
import org.flowable.engine.common.runtime.Clock;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.impl.FormEngineImpl;
import org.flowable.form.engine.impl.FormManagementServiceImpl;
import org.flowable.form.engine.impl.FormRepositoryServiceImpl;
import org.flowable.form.engine.impl.FormServiceImpl;
import org.flowable.form.engine.impl.ServiceImpl;
import org.flowable.form.engine.impl.cfg.CommandExecutorImpl;
import org.flowable.form.engine.impl.cfg.StandaloneFormEngineConfiguration;
import org.flowable.form.engine.impl.cfg.StandaloneInMemFormEngineConfiguration;
import org.flowable.form.engine.impl.cfg.TransactionListener;
import org.flowable.form.engine.impl.cfg.standalone.StandaloneMybatisTransactionContextFactory;
import org.flowable.form.engine.impl.db.DbSqlSessionFactory;
import org.flowable.form.engine.impl.deployer.CachingAndArtifactsManager;
import org.flowable.form.engine.impl.deployer.FormDefinitionDeployer;
import org.flowable.form.engine.impl.deployer.FormDefinitionDeploymentHelper;
import org.flowable.form.engine.impl.deployer.ParsedDeploymentBuilderFactory;
import org.flowable.form.engine.impl.el.ExpressionManager;
import org.flowable.form.engine.impl.interceptor.CommandContext;
import org.flowable.form.engine.impl.interceptor.CommandContextFactory;
import org.flowable.form.engine.impl.interceptor.CommandContextInterceptor;
import org.flowable.form.engine.impl.interceptor.CommandExecutor;
import org.flowable.form.engine.impl.interceptor.CommandInterceptor;
import org.flowable.form.engine.impl.interceptor.CommandInvoker;
import org.flowable.form.engine.impl.interceptor.LogInterceptor;
import org.flowable.form.engine.impl.interceptor.TransactionContextInterceptor;
import org.flowable.form.engine.impl.parser.FormDefinitionParseFactory;
import org.flowable.form.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.form.engine.impl.persistence.deploy.Deployer;
import org.flowable.form.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.form.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.form.engine.impl.persistence.deploy.FormDefinitionCacheEntry;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntityManager;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntityManagerImpl;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntityManager;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntityManagerImpl;
import org.flowable.form.engine.impl.persistence.entity.FormInstanceEntityManager;
import org.flowable.form.engine.impl.persistence.entity.FormInstanceEntityManagerImpl;
import org.flowable.form.engine.impl.persistence.entity.ResourceEntityManager;
import org.flowable.form.engine.impl.persistence.entity.ResourceEntityManagerImpl;
import org.flowable.form.engine.impl.persistence.entity.TableDataManager;
import org.flowable.form.engine.impl.persistence.entity.TableDataManagerImpl;
import org.flowable.form.engine.impl.persistence.entity.data.FormDefinitionDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.FormDeploymentDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.FormInstanceDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.ResourceDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.impl.MybatisFormDefinitionDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.impl.MybatisFormDeploymentDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.impl.MybatisFormInstanceDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.impl.MybatisResourceDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

public class FormEngineConfiguration extends AbstractEngineConfiguration {

    protected static final Logger logger = LoggerFactory.getLogger(FormEngineConfiguration.class);

    public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/flowable/form/db/mapping/mappings.xml";

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

    protected FormManagementService formManagementService = new FormManagementServiceImpl();
    protected FormRepositoryService formRepositoryService = new FormRepositoryServiceImpl();
    protected FormService formService = new FormServiceImpl();

    // DATA MANAGERS ///////////////////////////////////////////////////

    protected FormDeploymentDataManager deploymentDataManager;
    protected FormDefinitionDataManager formDefinitionDataManager;
    protected ResourceDataManager resourceDataManager;
    protected FormInstanceDataManager formInstanceDataManager;

    // ENTITY MANAGERS /////////////////////////////////////////////////
    protected FormDeploymentEntityManager deploymentEntityManager;
    protected FormDefinitionEntityManager formDefinitionEntityManager;
    protected ResourceEntityManager resourceEntityManager;
    protected FormInstanceEntityManager formInstanceEntityManager;
    protected TableDataManager tableDataManager;

    protected CommandContextFactory commandContextFactory;
    protected TransactionContextFactory<TransactionListener, CommandContext> transactionContextFactory;

    protected ExpressionManager expressionManager;

    protected FormJsonConverter formJsonConverter = new FormJsonConverter();

    // SESSION FACTORIES ///////////////////////////////////////////////
    protected DbSqlSessionFactory dbSqlSessionFactory;

    protected ObjectMapper objectMapper = new ObjectMapper();

    // DEPLOYERS
    // ////////////////////////////////////////////////////////////////

    protected FormDefinitionDeployer formDeployer;
    protected FormDefinitionParseFactory formParseFactory;
    protected ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory;
    protected FormDefinitionDeploymentHelper formDeploymentHelper;
    protected CachingAndArtifactsManager cachingAndArtifactsManager;
    protected List<Deployer> customPreDeployers;
    protected List<Deployer> customPostDeployers;
    protected List<Deployer> deployers;
    protected DeploymentManager deploymentManager;

    protected int formDefinitionCacheLimit = -1; // By default, no limit
    protected DeploymentCache<FormDefinitionCacheEntry> formDefinitionCache;

    public static FormEngineConfiguration createFormEngineConfigurationFromResourceDefault() {
        return createFormEngineConfigurationFromResource("flowable.form.cfg.xml", "formEngineConfiguration");
    }

    public static FormEngineConfiguration createFormEngineConfigurationFromResource(String resource) {
        return createFormEngineConfigurationFromResource(resource, "formEngineConfiguration");
    }

    public static FormEngineConfiguration createFormEngineConfigurationFromResource(String resource, String beanName) {
        return (FormEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromResource(resource, beanName);
    }

    public static FormEngineConfiguration createFormEngineConfigurationFromInputStream(InputStream inputStream) {
        return createFormEngineConfigurationFromInputStream(inputStream, "formEngineConfiguration");
    }

    public static FormEngineConfiguration createFormEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
        return (FormEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromInputStream(inputStream, beanName);
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

        initBeans();
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
        initService(formManagementService);
        initService(formRepositoryService);
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
        if (formDefinitionDataManager == null) {
            formDefinitionDataManager = new MybatisFormDefinitionDataManager(this);
        }
        if (resourceDataManager == null) {
            resourceDataManager = new MybatisResourceDataManager(this);
        }
        if (formInstanceDataManager == null) {
            formInstanceDataManager = new MybatisFormInstanceDataManager(this);
        }
    }

    public void initEntityManagers() {
        if (deploymentEntityManager == null) {
            deploymentEntityManager = new FormDeploymentEntityManagerImpl(this, deploymentDataManager);
        }
        if (formDefinitionEntityManager == null) {
            formDefinitionEntityManager = new FormDefinitionEntityManagerImpl(this, formDefinitionDataManager);
        }
        if (resourceEntityManager == null) {
            resourceEntityManager = new ResourceEntityManagerImpl(this, resourceDataManager);
        }
        if (formInstanceEntityManager == null) {
            formInstanceEntityManager = new FormInstanceEntityManagerImpl(this, formInstanceDataManager);
        }
        if (tableDataManager == null) {
            tableDataManager = new TableDataManagerImpl(this);
        }
    }

    // data model ///////////////////////////////////////////////////////////////

    public void initDbSchema() {
        try {
            DatabaseConnection connection = new JdbcConnection(dataSource.getConnection());
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
            database.setDatabaseChangeLogTableName(LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogTableName());
            database.setDatabaseChangeLogLockTableName(LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogLockTableName());

            if (StringUtils.isNotEmpty(databaseSchema)) {
                database.setDefaultSchemaName(databaseSchema);
                database.setLiquibaseSchemaName(databaseSchema);   
            }

            if (StringUtils.isNotEmpty(databaseCatalog)) {
                database.setDefaultCatalogName(databaseCatalog);
                database.setLiquibaseCatalogName(databaseCatalog);
            }

            Liquibase liquibase = new Liquibase("org/flowable/form/db/liquibase/flowable-form-db-changelog.xml", new ClassLoaderResourceAccessor(), database);

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
            throw new FlowableException("Error initialising form data schema", e);
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
        if (formParseFactory == null) {
            formParseFactory = new FormDefinitionParseFactory();
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
        if (formDefinitionCache == null) {
            if (formDefinitionCacheLimit <= 0) {
                formDefinitionCache = new DefaultDeploymentCache<FormDefinitionCacheEntry>();
            } else {
                formDefinitionCache = new DefaultDeploymentCache<FormDefinitionCacheEntry>(formDefinitionCacheLimit);
            }
        }

        deploymentManager = new DeploymentManager(formDefinitionCache, this);
        deploymentManager.setDeployers(deployers);
        deploymentManager.setDeploymentEntityManager(deploymentEntityManager);
        deploymentManager.setFormDefinitionEntityManager(formDefinitionEntityManager);
    }

    public Collection<? extends Deployer> getDefaultDeployers() {
        List<Deployer> defaultDeployers = new ArrayList<Deployer>();

        if (formDeployer == null) {
            formDeployer = new FormDefinitionDeployer();
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
            formDeploymentHelper = new FormDefinitionDeploymentHelper();
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
        return "org/flowable/form/db/properties/" + databaseType + ".properties";
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

    public FormEngineConfiguration setBeans(Map<Object, Object> beans) {
        this.beans = beans;
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

    public FormManagementService getFormManagementService() {
        return formManagementService;
    }

    public FormEngineConfiguration setFormManagementService(FormManagementService formManagementService) {
        this.formManagementService = formManagementService;
        return this;
    }

    public FormRepositoryService getFormRepositoryService() {
        return formRepositoryService;
    }

    public FormEngineConfiguration setFormRepositoryService(FormRepositoryService formRepositoryService) {
        this.formRepositoryService = formRepositoryService;
        return this;
    }

    public FormService getFormService() {
        return formService;
    }

    public FormEngineConfiguration setFormService(FormService formService) {
        this.formService = formService;
        return this;
    }

    public DeploymentManager getDeploymentManager() {
        return deploymentManager;
    }

    public FormEngineConfiguration getFormEngineConfiguration() {
        return this;
    }

    public FormDefinitionDeployer getFormDeployer() {
        return formDeployer;
    }

    public FormEngineConfiguration setFormDeployer(FormDefinitionDeployer formDeployer) {
        this.formDeployer = formDeployer;
        return this;
    }

    public FormDefinitionParseFactory getFormParseFactory() {
        return formParseFactory;
    }

    public FormEngineConfiguration setFormParseFactory(FormDefinitionParseFactory formParseFactory) {
        this.formParseFactory = formParseFactory;
        return this;
    }

    public int getFormCacheLimit() {
        return formDefinitionCacheLimit;
    }

    public FormEngineConfiguration setFormDefinitionCacheLimit(int formDefinitionCacheLimit) {
        this.formDefinitionCacheLimit = formDefinitionCacheLimit;
        return this;
    }

    public DeploymentCache<FormDefinitionCacheEntry> getFormDefinitionCache() {
        return formDefinitionCache;
    }

    public FormEngineConfiguration setFormDefinitionCache(DeploymentCache<FormDefinitionCacheEntry> formDefinitionCache) {
        this.formDefinitionCache = formDefinitionCache;
        return this;
    }

    public FormDeploymentDataManager getDeploymentDataManager() {
        return deploymentDataManager;
    }

    public FormEngineConfiguration setDeploymentDataManager(FormDeploymentDataManager deploymentDataManager) {
        this.deploymentDataManager = deploymentDataManager;
        return this;
    }

    public FormDefinitionDataManager getFormDefinitionDataManager() {
        return formDefinitionDataManager;
    }

    public FormEngineConfiguration setFormDefinitionDataManager(FormDefinitionDataManager formDefinitionDataManager) {
        this.formDefinitionDataManager = formDefinitionDataManager;
        return this;
    }

    public ResourceDataManager getResourceDataManager() {
        return resourceDataManager;
    }

    public FormEngineConfiguration setResourceDataManager(ResourceDataManager resourceDataManager) {
        this.resourceDataManager = resourceDataManager;
        return this;
    }

    public FormInstanceDataManager getFormInstanceDataManager() {
        return formInstanceDataManager;
    }

    public FormEngineConfiguration setFormInstanceDataManager(FormInstanceDataManager formInstanceDataManager) {
        this.formInstanceDataManager = formInstanceDataManager;
        return this;
    }

    public FormDeploymentEntityManager getDeploymentEntityManager() {
        return deploymentEntityManager;
    }

    public FormEngineConfiguration setDeploymentEntityManager(FormDeploymentEntityManager deploymentEntityManager) {
        this.deploymentEntityManager = deploymentEntityManager;
        return this;
    }

    public FormDefinitionEntityManager getFormDefinitionEntityManager() {
        return formDefinitionEntityManager;
    }

    public FormEngineConfiguration setFormDefinitionEntityManager(FormDefinitionEntityManager formDefinitionEntityManager) {
        this.formDefinitionEntityManager = formDefinitionEntityManager;
        return this;
    }

    public ResourceEntityManager getResourceEntityManager() {
        return resourceEntityManager;
    }

    public FormEngineConfiguration setResourceEntityManager(ResourceEntityManager resourceEntityManager) {
        this.resourceEntityManager = resourceEntityManager;
        return this;
    }

    public FormInstanceEntityManager getFormInstanceEntityManager() {
        return formInstanceEntityManager;
    }

    public FormEngineConfiguration setFormInstanceEntityManager(FormInstanceEntityManager formInstanceEntityManager) {
        this.formInstanceEntityManager = formInstanceEntityManager;
        return this;
    }

    public TableDataManager getTableDataManager() {
        return tableDataManager;
    }

    public FormEngineConfiguration setTableDataManager(TableDataManager tableDataManager) {
        this.tableDataManager = tableDataManager;
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
