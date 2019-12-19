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
package org.flowable.eventregistry.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.HasExpressionManagerEngineConfiguration;
import org.flowable.common.engine.impl.cfg.BeansConfigurationHelper;
import org.flowable.common.engine.impl.db.DbSqlSessionFactory;
import org.flowable.common.engine.impl.el.DefaultExpressionManager;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryConfigurationApi;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.InboundEventProcessor;
import org.flowable.eventregistry.api.OutboundEventProcessor;
import org.flowable.eventregistry.impl.cfg.StandaloneEventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.cfg.StandaloneInMemEventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.cmd.SchemaOperationsEventRegistryEngineBuild;
import org.flowable.eventregistry.impl.db.EntityDependencyOrder;
import org.flowable.eventregistry.impl.db.EventDbSchemaManager;
import org.flowable.eventregistry.impl.deployer.CachingAndArtifactsManager;
import org.flowable.eventregistry.impl.deployer.ChannelDefinitionProcessor;
import org.flowable.eventregistry.impl.deployer.EventDefinitionDeployer;
import org.flowable.eventregistry.impl.deployer.EventDefinitionDeploymentHelper;
import org.flowable.eventregistry.impl.deployer.ParsedDeploymentBuilderFactory;
import org.flowable.eventregistry.impl.parser.EventDefinitionParseFactory;
import org.flowable.eventregistry.impl.persistence.deploy.Deployer;
import org.flowable.eventregistry.impl.persistence.deploy.EventDefinitionCacheEntry;
import org.flowable.eventregistry.impl.persistence.deploy.EventDeploymentManager;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntityManagerImpl;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntityManagerImpl;
import org.flowable.eventregistry.impl.persistence.entity.EventResourceEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.EventResourceEntityManagerImpl;
import org.flowable.eventregistry.impl.persistence.entity.TableDataManager;
import org.flowable.eventregistry.impl.persistence.entity.TableDataManagerImpl;
import org.flowable.eventregistry.impl.persistence.entity.data.EventDefinitionDataManager;
import org.flowable.eventregistry.impl.persistence.entity.data.EventDeploymentDataManager;
import org.flowable.eventregistry.impl.persistence.entity.data.EventResourceDataManager;
import org.flowable.eventregistry.impl.persistence.entity.data.impl.MybatisEventDefinitionDataManager;
import org.flowable.eventregistry.impl.persistence.entity.data.impl.MybatisEventDeploymentDataManager;
import org.flowable.eventregistry.impl.persistence.entity.data.impl.MybatisEventResourceDataManager;
import org.flowable.eventregistry.json.converter.EventJsonConverter;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;

public class EventRegistryEngineConfiguration extends AbstractEngineConfiguration
        implements EventRegistryConfigurationApi, HasExpressionManagerEngineConfiguration {

    public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/flowable/eventregistry/db/mapping/mappings.xml";

    public static final String LIQUIBASE_CHANGELOG_PREFIX = "FLW_EV_";

    protected String eventRegistryEngineName = EventRegistryEngines.NAME_DEFAULT;

    // SERVICES
    // /////////////////////////////////////////////////////////////////

    protected EventRepositoryService eventRepositoryService = new EventRepositoryServiceImpl(this);

    // DATA MANAGERS ///////////////////////////////////////////////////

    protected EventDeploymentDataManager deploymentDataManager;
    protected EventDefinitionDataManager eventDefinitionDataManager;
    protected EventResourceDataManager resourceDataManager;

    // ENTITY MANAGERS /////////////////////////////////////////////////
    protected EventDeploymentEntityManager deploymentEntityManager;
    protected EventDefinitionEntityManager eventDefinitionEntityManager;
    protected EventResourceEntityManager resourceEntityManager;
    protected TableDataManager tableDataManager;

    protected ExpressionManager expressionManager;

    protected EventJsonConverter eventJsonConverter = new EventJsonConverter();

    // DEPLOYERS
    // ////////////////////////////////////////////////////////////////

    protected EventDefinitionDeployer eventDeployer;
    protected EventDefinitionParseFactory eventParseFactory;
    protected ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory;
    protected EventDefinitionDeploymentHelper eventDeploymentHelper;
    protected CachingAndArtifactsManager cachingAndArtifactsManager;
    protected List<Deployer> customPreDeployers;
    protected List<Deployer> customPostDeployers;
    protected List<Deployer> deployers;
    protected EventDeploymentManager deploymentManager;

    protected int eventDefinitionCacheLimit = -1; // By default, no limit
    protected DeploymentCache<EventDefinitionCacheEntry> eventDefinitionCache;
    
    protected Collection<ChannelDefinitionProcessor> channelDefinitionProcessors = new ArrayList<>();

    // Event registry
    protected EventRegistry eventRegistry;
    protected InboundEventProcessor inboundEventProcessor;
    protected OutboundEventProcessor outboundEventProcessor;

    public static EventRegistryEngineConfiguration createEventRegistryEngineConfigurationFromResourceDefault() {
        return createEventRegistryEngineConfigurationFromResource("flowable.eventregistry.cfg.xml", "eventRegistryEngineConfiguration");
    }

    public static EventRegistryEngineConfiguration createEventRegistryEngineConfigurationFromResource(String resource) {
        return createEventRegistryEngineConfigurationFromResource(resource, "eventRegistryEngineConfiguration");
    }

    public static EventRegistryEngineConfiguration createEventRegistryEngineConfigurationFromResource(String resource, String beanName) {
        return (EventRegistryEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromResource(resource, beanName);
    }

    public static EventRegistryEngineConfiguration createEventRegistryEngineConfigurationFromInputStream(InputStream inputStream) {
        return createEventRegistryEngineConfigurationFromInputStream(inputStream, "eventRegistryEngineConfiguration");
    }

    public static EventRegistryEngineConfiguration createEventRegistryEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
        return (EventRegistryEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromInputStream(inputStream, beanName);
    }

    public static EventRegistryEngineConfiguration createStandaloneEventRegistryEngineConfiguration() {
        return new StandaloneEventRegistryEngineConfiguration();
    }

    public static EventRegistryEngineConfiguration createStandaloneInMemEventRegistryEngineConfiguration() {
        return new StandaloneInMemEventRegistryEngineConfiguration();
    }

    // buildFormEngine
    // ///////////////////////////////////////////////////////

    public EventRegistryEngine buildEventRegistryEngine() {
        init();
        return new EventRegistryEngineImpl(this);
    }

    // init
    // /////////////////////////////////////////////////////////////////////

    protected void init() {
        initEngineConfigurations();
        initConfigurators();
        configuratorsBeforeInit();
        initExpressionManager();
        initCommandContextFactory();
        initTransactionContextFactory();
        initCommandExecutors();
        initIdGenerator();

        if (usingRelationalDatabase) {
            initDataSource();
        }
        
        if (usingRelationalDatabase || usingSchemaMgmt) {
            initSchemaManager();
            initSchemaManagementCommand();
        }

        initBeans();
        initTransactionFactory();

        if (usingRelationalDatabase) {
            initSqlSessionFactory();
        }

        initSessionFactories();
        initServices();
        configuratorsAfterInit();
        initDataManagers();
        initEntityManagers();
        initEventRegistry();
        initInboundEventProcessor();
        initOutboundEventProcessor();
        initDeployers();
        initClock();
    }

    // services
    // /////////////////////////////////////////////////////////////////

    protected void initServices() {
        initService(eventRepositoryService);
    }

    public void initExpressionManager() {
        if (expressionManager == null) {
            expressionManager = new DefaultExpressionManager();
        }
    }

    // Data managers
    ///////////////////////////////////////////////////////////

    @Override
    public void initDataManagers() {
        super.initDataManagers();
        if (deploymentDataManager == null) {
            deploymentDataManager = new MybatisEventDeploymentDataManager(this);
        }
        if (eventDefinitionDataManager == null) {
            eventDefinitionDataManager = new MybatisEventDefinitionDataManager(this);
        }
        if (resourceDataManager == null) {
            resourceDataManager = new MybatisEventResourceDataManager(this);
        }
    }

    @Override
    public void initEntityManagers() {
        super.initEntityManagers();
        if (deploymentEntityManager == null) {
            deploymentEntityManager = new EventDeploymentEntityManagerImpl(this, deploymentDataManager);
        }
        if (eventDefinitionEntityManager == null) {
            eventDefinitionEntityManager = new EventDefinitionEntityManagerImpl(this, eventDefinitionDataManager);
        }
        if (resourceEntityManager == null) {
            resourceEntityManager = new EventResourceEntityManagerImpl(this, resourceDataManager);
        }
        if (tableDataManager == null) {
            tableDataManager = new TableDataManagerImpl(this);
        }
    }

    // data model ///////////////////////////////////////////////////////////////

    @Override
    public void initSchemaManager() {
        super.initSchemaManager();
        if (this.schemaManager == null) {
            this.schemaManager = new EventDbSchemaManager();
        }
    }
    
    public void initSchemaManagementCommand() {
        if (schemaManagementCmd == null) {
            if (usingRelationalDatabase && databaseSchemaUpdate != null) {
                this.schemaManagementCmd = new SchemaOperationsEventRegistryEngineBuild();
            }
        }
    }

    private void closeDatabase(Liquibase liquibase) {
        if (liquibase != null) {
            Database database = liquibase.getDatabase();
            if (database != null) {
                try {
                    database.close();
                } catch (DatabaseException e) {
                    logger.warn("Error closing database", e);
                }
            }
        }
    }

    // session factories ////////////////////////////////////////////////////////

    @Override
    public void initDbSqlSessionFactory() {
        if (dbSqlSessionFactory == null) {
            dbSqlSessionFactory = createDbSqlSessionFactory();
            dbSqlSessionFactory.setDatabaseType(databaseType);
            dbSqlSessionFactory.setSqlSessionFactory(sqlSessionFactory);
            dbSqlSessionFactory.setDatabaseTablePrefix(databaseTablePrefix);
            dbSqlSessionFactory.setTablePrefixIsSchema(tablePrefixIsSchema);
            dbSqlSessionFactory.setDatabaseCatalog(databaseCatalog);
            dbSqlSessionFactory.setDatabaseSchema(databaseSchema);
            addSessionFactory(dbSqlSessionFactory);
        }
        initDbSqlSessionFactoryEntitySettings();
    }

    @Override
    protected void initDbSqlSessionFactoryEntitySettings() {
        defaultInitDbSqlSessionFactoryEntitySettings(EntityDependencyOrder.INSERT_ORDER, EntityDependencyOrder.DELETE_ORDER);
    }

    @Override
    public DbSqlSessionFactory createDbSqlSessionFactory() {
        return new DbSqlSessionFactory(usePrefixId);
    }

    // command executors
    // ////////////////////////////////////////////////////////

    @Override
    public void initCommandExecutors() {
        initDefaultCommandConfig();
        initSchemaCommandConfig();
        initCommandInvoker();
        initCommandInterceptors();
        initCommandExecutor();
    }

    @Override
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

    @Override
    public String getEngineCfgKey() {
        return EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG;
    }

    @Override
    public CommandInterceptor createTransactionInterceptor() {
        return null;
    }

    // deployers
    // ////////////////////////////////////////////////////////////////

    protected void initDeployers() {
        if (eventParseFactory == null) {
            eventParseFactory = new EventDefinitionParseFactory();
        }

        if (this.eventDeployer == null) {
            this.deployers = new ArrayList<>();
            if (customPreDeployers != null) {
                this.deployers.addAll(customPreDeployers);
            }
            this.deployers.addAll(getDefaultDeployers());
            if (customPostDeployers != null) {
                this.deployers.addAll(customPostDeployers);
            }
        }

        if (eventDefinitionCache == null) {
            if (eventDefinitionCacheLimit <= 0) {
                eventDefinitionCache = new DefaultDeploymentCache<>();
            } else {
                eventDefinitionCache = new DefaultDeploymentCache<>(eventDefinitionCacheLimit);
            }
        }

        deploymentManager = new EventDeploymentManager(eventDefinitionCache, this);
        deploymentManager.setDeployers(deployers);
        deploymentManager.setDeploymentEntityManager(deploymentEntityManager);
        deploymentManager.setEventDefinitionEntityManager(eventDefinitionEntityManager);
    }

    public Collection<? extends Deployer> getDefaultDeployers() {
        List<Deployer> defaultDeployers = new ArrayList<>();

        if (eventDeployer == null) {
            eventDeployer = new EventDefinitionDeployer();
        }

        initEventDeployerDependencies();

        eventDeployer.setIdGenerator(idGenerator);
        eventDeployer.setParsedDeploymentBuilderFactory(parsedDeploymentBuilderFactory);
        eventDeployer.setEventDeploymentHelper(eventDeploymentHelper);
        eventDeployer.setCachingAndArtifactsManager(cachingAndArtifactsManager);
        eventDeployer.setUsePrefixId(usePrefixId);

        defaultDeployers.add(eventDeployer);
        return defaultDeployers;
    }

    public void initEventDeployerDependencies() {
        if (parsedDeploymentBuilderFactory == null) {
            parsedDeploymentBuilderFactory = new ParsedDeploymentBuilderFactory();
        }
        if (parsedDeploymentBuilderFactory.getEventParseFactory() == null) {
            parsedDeploymentBuilderFactory.setEventParseFactory(eventParseFactory);
        }

        if (eventDeploymentHelper == null) {
            eventDeploymentHelper = new EventDefinitionDeploymentHelper();
        }

        if (cachingAndArtifactsManager == null) {
            cachingAndArtifactsManager = new CachingAndArtifactsManager();
        }
    }
    
    public void initEventRegistry() {
        if (this.eventRegistry == null) {
            this.eventRegistry = new DefaultEventRegistry(this);
        }
    }

    public void initInboundEventProcessor() {
        if (this.inboundEventProcessor == null) {
            this.inboundEventProcessor = new DefaultInboundEventProcessor(eventRegistry);
        }
        this.eventRegistry.setInboundEventProcessor(this.inboundEventProcessor);
    }

    public void initOutboundEventProcessor() {
        if (this.outboundEventProcessor == null) {
            this.outboundEventProcessor = new DefaultOutboundEventProcessor(eventRegistry);
        }
        this.eventRegistry.setOutboundEventProcessor(outboundEventProcessor);
    }

    // myBatis SqlSessionFactory
    // ////////////////////////////////////////////////

    @Override
    public InputStream getMyBatisXmlConfigurationStream() {
        return getResourceAsStream(DEFAULT_MYBATIS_MAPPING_FILE);
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    @Override
    public String getEngineName() {
        return eventRegistryEngineName;
    }

    public EventRegistryEngineConfiguration setEngineName(String eventRegistryEngineName) {
        this.eventRegistryEngineName = eventRegistryEngineName;
        return this;
    }

    @Override
    public EventRepositoryService getEventRepositoryService() {
        return eventRepositoryService;
    }

    public EventRegistryEngineConfiguration setEventRepositoryService(EventRepositoryService eventRepositoryService) {
        this.eventRepositoryService = eventRepositoryService;
        return this;
    }

    public EventDeploymentManager getDeploymentManager() {
        return deploymentManager;
    }

    public EventRegistryEngineConfiguration getFormEngineConfiguration() {
        return this;
    }

    public EventDefinitionDeployer getEventDeployer() {
        return eventDeployer;
    }

    public EventRegistryEngineConfiguration setEventDeployer(EventDefinitionDeployer eventDeployer) {
        this.eventDeployer = eventDeployer;
        return this;
    }

    public EventDefinitionParseFactory getEventParseFactory() {
        return eventParseFactory;
    }

    public EventRegistryEngineConfiguration setEventParseFactory(EventDefinitionParseFactory eventParseFactory) {
        this.eventParseFactory = eventParseFactory;
        return this;
    }
    
    public EventRegistry getEventRegistry() {
        return eventRegistry;
    }

    public EventRegistryEngineConfiguration setEventRegistry(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
        return this;
    }

    public InboundEventProcessor getInboundEventProcessor() {
        return inboundEventProcessor;
    }

    public EventRegistryEngineConfiguration setInboundEventProcessor(InboundEventProcessor inboundEventProcessor) {
        this.inboundEventProcessor = inboundEventProcessor;
        return this;
    }

    public OutboundEventProcessor getOutboundEventProcessor() {
        return outboundEventProcessor;
    }

    public EventRegistryEngineConfiguration setOutboundEventProcessor(OutboundEventProcessor outboundEventProcessor) {
        this.outboundEventProcessor = outboundEventProcessor;
        return this;
    }

    public int getEventDefinitionCacheLimit() {
        return eventDefinitionCacheLimit;
    }

    public EventRegistryEngineConfiguration setEventDefinitionCacheLimit(int eventDefinitionCacheLimit) {
        this.eventDefinitionCacheLimit = eventDefinitionCacheLimit;
        return this;
    }

    public DeploymentCache<EventDefinitionCacheEntry> getEventDefinitionCache() {
        return eventDefinitionCache;
    }

    public EventRegistryEngineConfiguration setEventDefinitionCache(DeploymentCache<EventDefinitionCacheEntry> eventDefinitionCache) {
        this.eventDefinitionCache = eventDefinitionCache;
        return this;
    }

    public Collection<ChannelDefinitionProcessor> getChannelDefinitionProcessors() {
        return channelDefinitionProcessors;
    }

    public void addChannelDefinitionProcessor(ChannelDefinitionProcessor channelDefinitionProcessor) {
        if (this.channelDefinitionProcessors == null) {
            this.channelDefinitionProcessors = new ArrayList<>();
        }
        this.channelDefinitionProcessors.add(channelDefinitionProcessor);
    }

    public void setChannelDefinitionProcessors(Collection<ChannelDefinitionProcessor> channelDefinitionProcessors) {
        this.channelDefinitionProcessors = channelDefinitionProcessors;
    }

    public EventDeploymentDataManager getDeploymentDataManager() {
        return deploymentDataManager;
    }

    public EventRegistryEngineConfiguration setDeploymentDataManager(EventDeploymentDataManager deploymentDataManager) {
        this.deploymentDataManager = deploymentDataManager;
        return this;
    }

    public EventDefinitionDataManager getEventDefinitionDataManager() {
        return eventDefinitionDataManager;
    }

    public EventRegistryEngineConfiguration setEventDefinitionDataManager(EventDefinitionDataManager eventDefinitionDataManager) {
        this.eventDefinitionDataManager = eventDefinitionDataManager;
        return this;
    }

    public EventResourceDataManager getResourceDataManager() {
        return resourceDataManager;
    }

    public EventRegistryEngineConfiguration setResourceDataManager(EventResourceDataManager resourceDataManager) {
        this.resourceDataManager = resourceDataManager;
        return this;
    }

    public EventDeploymentEntityManager getDeploymentEntityManager() {
        return deploymentEntityManager;
    }

    public EventRegistryEngineConfiguration setDeploymentEntityManager(EventDeploymentEntityManager deploymentEntityManager) {
        this.deploymentEntityManager = deploymentEntityManager;
        return this;
    }

    public EventDefinitionEntityManager getEventDefinitionEntityManager() {
        return eventDefinitionEntityManager;
    }

    public EventRegistryEngineConfiguration setEventDefinitionEntityManager(EventDefinitionEntityManager eventDefinitionEntityManager) {
        this.eventDefinitionEntityManager = eventDefinitionEntityManager;
        return this;
    }

    public EventResourceEntityManager getResourceEntityManager() {
        return resourceEntityManager;
    }

    public EventRegistryEngineConfiguration setResourceEntityManager(EventResourceEntityManager resourceEntityManager) {
        this.resourceEntityManager = resourceEntityManager;
        return this;
    }

    public TableDataManager getTableDataManager() {
        return tableDataManager;
    }

    public EventRegistryEngineConfiguration setTableDataManager(TableDataManager tableDataManager) {
        this.tableDataManager = tableDataManager;
        return this;
    }

    @Override
    public ExpressionManager getExpressionManager() {
        return expressionManager;
    }

    @Override
    public EventRegistryEngineConfiguration setExpressionManager(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
        return this;
    }

    public EventJsonConverter getEventJsonConverter() {
        return eventJsonConverter;
    }

    public EventRegistryEngineConfiguration setEventJsonConverter(EventJsonConverter eventJsonConverter) {
        this.eventJsonConverter = eventJsonConverter;
        return this;
    }
}
