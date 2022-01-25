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

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.HasExpressionManagerEngineConfiguration;
import org.flowable.common.engine.impl.cfg.BeansConfigurationHelper;
import org.flowable.common.engine.impl.db.DbSqlSessionFactory;
import org.flowable.common.engine.impl.el.DefaultExpressionManager;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.flowable.common.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.common.engine.impl.persistence.deploy.FullDeploymentCache;
import org.flowable.common.engine.impl.persistence.entity.TableDataManager;
import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.flowable.eventregistry.api.EventManagementService;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryConfigurationApi;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.InboundEventProcessor;
import org.flowable.eventregistry.api.EventRegistryNonMatchingEventConsumer;
import org.flowable.eventregistry.api.OutboundEventProcessor;
import org.flowable.eventregistry.api.management.EventRegistryChangeDetectionExecutor;
import org.flowable.eventregistry.api.management.EventRegistryChangeDetectionManager;
import org.flowable.eventregistry.impl.cfg.StandaloneEventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.cfg.StandaloneInMemEventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.cmd.SchemaOperationsEventRegistryEngineBuild;
import org.flowable.eventregistry.impl.db.EntityDependencyOrder;
import org.flowable.eventregistry.impl.db.EventDbSchemaManager;
import org.flowable.eventregistry.impl.deployer.CachingAndArtifactsManager;
import org.flowable.eventregistry.impl.deployer.ChannelDefinitionDeploymentHelper;
import org.flowable.eventregistry.impl.deployer.EventDefinitionDeployer;
import org.flowable.eventregistry.impl.deployer.EventDefinitionDeploymentHelper;
import org.flowable.eventregistry.impl.deployer.ParsedDeploymentBuilderFactory;
import org.flowable.eventregistry.impl.management.DefaultEventRegistryChangeDetectionExecutor;
import org.flowable.eventregistry.impl.management.DefaultEventRegistryChangeDetectionManager;
import org.flowable.eventregistry.impl.parser.ChannelDefinitionParseFactory;
import org.flowable.eventregistry.impl.parser.EventDefinitionParseFactory;
import org.flowable.eventregistry.impl.persistence.deploy.ChannelDefinitionCacheEntry;
import org.flowable.eventregistry.impl.persistence.deploy.Deployer;
import org.flowable.eventregistry.impl.persistence.deploy.EventDefinitionCacheEntry;
import org.flowable.eventregistry.impl.persistence.deploy.EventDeploymentManager;
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntityManagerImpl;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntityManagerImpl;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntityManagerImpl;
import org.flowable.eventregistry.impl.persistence.entity.EventResourceEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.EventResourceEntityManagerImpl;
import org.flowable.eventregistry.impl.persistence.entity.data.ChannelDefinitionDataManager;
import org.flowable.eventregistry.impl.persistence.entity.data.EventDefinitionDataManager;
import org.flowable.eventregistry.impl.persistence.entity.data.EventDeploymentDataManager;
import org.flowable.eventregistry.impl.persistence.entity.data.EventResourceDataManager;
import org.flowable.eventregistry.impl.persistence.entity.data.impl.MybatisChannelDefinitionDataManager;
import org.flowable.eventregistry.impl.persistence.entity.data.impl.MybatisEventDefinitionDataManager;
import org.flowable.eventregistry.impl.persistence.entity.data.impl.MybatisEventDeploymentDataManager;
import org.flowable.eventregistry.impl.persistence.entity.data.impl.MybatisEventResourceDataManager;
import org.flowable.eventregistry.impl.pipeline.DelegateExpressionInboundChannelModelProcessor;
import org.flowable.eventregistry.impl.pipeline.DelegateExpressionOutboundChannelModelProcessor;
import org.flowable.eventregistry.impl.pipeline.InMemoryOutboundEventChannelAdapter;
import org.flowable.eventregistry.impl.pipeline.InMemoryOutboundEventProcessingPipeline;
import org.flowable.eventregistry.impl.pipeline.InboundChannelModelProcessor;
import org.flowable.eventregistry.impl.pipeline.OutboundChannelModelProcessor;
import org.flowable.eventregistry.json.converter.ChannelJsonConverter;
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
    protected EventManagementService eventManagementService = new EventManagementServiceImpl(this);

    // DATA MANAGERS ///////////////////////////////////////////////////

    protected EventDeploymentDataManager deploymentDataManager;
    protected EventDefinitionDataManager eventDefinitionDataManager;
    protected ChannelDefinitionDataManager channelDefinitionDataManager;
    protected EventResourceDataManager resourceDataManager;

    // ENTITY MANAGERS /////////////////////////////////////////////////
    protected EventDeploymentEntityManager deploymentEntityManager;
    protected EventDefinitionEntityManager eventDefinitionEntityManager;
    protected ChannelDefinitionEntityManager channelDefinitionEntityManager;
    protected EventResourceEntityManager resourceEntityManager;

    protected ExpressionManager expressionManager;
    protected Collection<ELResolver> preDefaultELResolvers;
    protected Collection<ELResolver> preBeanELResolvers;
    protected Collection<ELResolver> postDefaultELResolvers;

    protected EventJsonConverter eventJsonConverter = new EventJsonConverter();
    protected ChannelJsonConverter channelJsonConverter = new ChannelJsonConverter();

    // DEPLOYERS
    // ////////////////////////////////////////////////////////////////

    protected EventDefinitionDeployer eventDeployer;
    protected EventDefinitionParseFactory eventParseFactory;
    protected ChannelDefinitionParseFactory channelParseFactory;
    protected ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory;
    protected EventDefinitionDeploymentHelper eventDeploymentHelper;
    protected ChannelDefinitionDeploymentHelper channelDeploymentHelper;
    protected CachingAndArtifactsManager cachingAndArtifactsManager;
    protected List<Deployer> customPreDeployers;
    protected List<Deployer> customPostDeployers;
    protected List<Deployer> deployers;
    protected EventDeploymentManager deploymentManager;

    protected int eventDefinitionCacheLimit = -1; // By default, no limit
    protected DeploymentCache<EventDefinitionCacheEntry> eventDefinitionCache;
    protected DeploymentCache<ChannelDefinitionCacheEntry> channelDefinitionCache;

    protected Collection<ChannelModelProcessor> channelModelProcessors = new ArrayList<>();

    // Event registry
    protected EventRegistry eventRegistry;
    protected InboundEventProcessor inboundEventProcessor;
    protected OutboundEventProcessor outboundEventProcessor;
    protected OutboundEventProcessor systemOutboundEventProcessor;

    // Change detection
    protected boolean enableEventRegistryChangeDetection;
    protected long eventRegistryChangeDetectionInitialDelayInMs = 10000L;
    protected long eventRegistryChangeDetectionDelayInMs = 60000L;
    protected EventRegistryChangeDetectionManager eventRegistryChangeDetectionManager;
    protected EventRegistryChangeDetectionExecutor eventRegistryChangeDetectionExecutor;
    
    protected EventRegistryNonMatchingEventConsumer nonMatchingEventConsumer;

    protected boolean enableEventRegistryChangeDetectionAfterEngineCreate = true;

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

    // buildEventRegistryEngine
    // ///////////////////////////////////////////////////////

    public EventRegistryEngine buildEventRegistryEngine() {
        init();
        EventRegistryEngineImpl eventRegistryEngine = new EventRegistryEngineImpl(this);

        if (enableEventRegistryChangeDetectionAfterEngineCreate) {

            eventRegistryEngine.handleDeployedChannelDefinitions();

            if (enableEventRegistryChangeDetection) {
                eventRegistryChangeDetectionExecutor.initialize();
            }
        }

        return eventRegistryEngine;
    }

    // init
    // /////////////////////////////////////////////////////////////////////

    protected void init() {
        initEngineConfigurations();
        initConfigurators();
        configuratorsBeforeInit();
        initClock();
        initObjectMapper();
        initBeans();
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
        initSystemOutboundEventProcessor();
        initChannelDefinitionProcessors();
        initDeployers();
        initChangeDetectionManager();
        initChangeDetectionExecutor();
    }

    // services
    // /////////////////////////////////////////////////////////////////

    protected void initServices() {
        initService(eventRepositoryService);
        initService(eventManagementService);
    }

    public void initExpressionManager() {
        if (expressionManager == null) {
            DefaultExpressionManager eventRegistryExpressionManager = new DefaultExpressionManager(beans);

            if (preDefaultELResolvers != null) {
                preDefaultELResolvers.forEach(eventRegistryExpressionManager::addPreDefaultResolver);
            }

            if (preBeanELResolvers != null) {
                preBeanELResolvers.forEach(eventRegistryExpressionManager::addPreBeanResolver);
            }

            if (postDefaultELResolvers != null) {
                postDefaultELResolvers.forEach(eventRegistryExpressionManager::addPostDefaultResolver);
            }

            expressionManager = eventRegistryExpressionManager;
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
        if (channelDefinitionDataManager == null) {
            channelDefinitionDataManager = new MybatisChannelDefinitionDataManager(this);
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
        if (channelDefinitionEntityManager == null) {
            channelDefinitionEntityManager = new ChannelDefinitionEntityManagerImpl(this, channelDefinitionDataManager);
        }
        if (resourceEntityManager == null) {
            resourceEntityManager = new EventResourceEntityManagerImpl(this, resourceDataManager);
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
    public String getEngineScopeType() {
        return ScopeTypes.EVENT_REGISTRY;
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
        
        if (channelParseFactory == null) {
            channelParseFactory = new ChannelDefinitionParseFactory();
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
        
        if (channelDefinitionCache == null) {
            channelDefinitionCache = new FullDeploymentCache<>();
        }

        deploymentManager = new EventDeploymentManager(eventDefinitionCache, channelDefinitionCache, this);
        deploymentManager.setDeployers(deployers);
        deploymentManager.setDeploymentEntityManager(deploymentEntityManager);
        deploymentManager.setEventDefinitionEntityManager(eventDefinitionEntityManager);
        deploymentManager.setChannelDefinitionEntityManager(channelDefinitionEntityManager);
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
        eventDeployer.setChannelDeploymentHelper(channelDeploymentHelper);
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
        
        if (parsedDeploymentBuilderFactory.getChannelParseFactory() == null) {
            parsedDeploymentBuilderFactory.setChannelParseFactory(channelParseFactory);
        }

        if (eventDeploymentHelper == null) {
            eventDeploymentHelper = new EventDefinitionDeploymentHelper();
        }
        
        if (channelDeploymentHelper == null) {
            channelDeploymentHelper = new ChannelDefinitionDeploymentHelper();
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
            this.outboundEventProcessor = new DefaultOutboundEventProcessor(eventRepositoryService, fallbackToDefaultTenant);
        }
        this.eventRegistry.setOutboundEventProcessor(outboundEventProcessor);
    }

    public void initSystemOutboundEventProcessor() {
        if (this.systemOutboundEventProcessor == null) {
            this.systemOutboundEventProcessor = new SystemOutboundEventProcessor<>(
                    new InMemoryOutboundEventChannelAdapter(eventRegistry),
                    new InMemoryOutboundEventProcessingPipeline()
            );
        }
        this.eventRegistry.setSystemOutboundEventProcessor(systemOutboundEventProcessor);
    }

    public void initChannelDefinitionProcessors() {
        channelModelProcessors.add(new DelegateExpressionInboundChannelModelProcessor(this));
        channelModelProcessors.add(new DelegateExpressionOutboundChannelModelProcessor(this));
        channelModelProcessors.add(new InboundChannelModelProcessor());
        channelModelProcessors.add(new OutboundChannelModelProcessor());
    }

    public void initChangeDetectionManager() {
        if (this.eventRegistryChangeDetectionManager == null) {
            this.eventRegistryChangeDetectionManager = new DefaultEventRegistryChangeDetectionManager(this);
        }
    }

    public void initChangeDetectionExecutor() {
        if (this.eventRegistryChangeDetectionExecutor == null) {
            this.eventRegistryChangeDetectionExecutor = new DefaultEventRegistryChangeDetectionExecutor(eventRegistryChangeDetectionManager,
                eventRegistryChangeDetectionInitialDelayInMs, eventRegistryChangeDetectionDelayInMs);
        }
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

    @Override
    public EventManagementService getEventManagementService() {
        return eventManagementService;
    }

    public EventRegistryEngineConfiguration setEventManagementService(EventManagementService eventManagementService) {
        this.eventManagementService = eventManagementService;
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
    
    @Override
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

    public OutboundEventProcessor getSystemOutboundEventProcessor() {
        return systemOutboundEventProcessor;
    }

    public EventRegistryEngineConfiguration setSystemOutboundEventProcessor(OutboundEventProcessor systemOutboundEventProcessor) {
        this.systemOutboundEventProcessor = systemOutboundEventProcessor;
        return this;
    }

    public boolean isEnableEventRegistryChangeDetection() {
        return enableEventRegistryChangeDetection;
    }

    public EventRegistryEngineConfiguration setEnableEventRegistryChangeDetection(boolean enableEventRegistryChangeDetection) {
        this.enableEventRegistryChangeDetection = enableEventRegistryChangeDetection;
        return this;
    }

    public long getEventRegistryChangeDetectionInitialDelayInMs() {
        return eventRegistryChangeDetectionInitialDelayInMs;
    }

    public EventRegistryEngineConfiguration setEventRegistryChangeDetectionInitialDelayInMs(long eventRegistryChangeDetectionInitialDelayInMs) {
        this.eventRegistryChangeDetectionInitialDelayInMs = eventRegistryChangeDetectionInitialDelayInMs;
        return this;
    }

    public long getEventRegistryChangeDetectionDelayInMs() {
        return eventRegistryChangeDetectionDelayInMs;
    }

    public EventRegistryEngineConfiguration setEventRegistryChangeDetectionDelayInMs(long eventRegistryChangeDetectionDelayInMs) {
        this.eventRegistryChangeDetectionDelayInMs = eventRegistryChangeDetectionDelayInMs;
        return this;
    }

    public EventRegistryChangeDetectionManager getEventRegistryChangeDetectionManager() {
        return eventRegistryChangeDetectionManager;
    }

    public EventRegistryEngineConfiguration setEventRegistryChangeDetectionManager(EventRegistryChangeDetectionManager eventRegistryChangeDetectionManager) {
        this.eventRegistryChangeDetectionManager = eventRegistryChangeDetectionManager;
        return this;
    }

    public EventRegistryChangeDetectionExecutor getEventRegistryChangeDetectionExecutor() {
        return eventRegistryChangeDetectionExecutor;
    }
    public EventRegistryEngineConfiguration setEventRegistryChangeDetectionExecutor(EventRegistryChangeDetectionExecutor eventRegistryChangeDetectionExecutor) {
        this.eventRegistryChangeDetectionExecutor = eventRegistryChangeDetectionExecutor;
        return this;
    }

    public EventRegistryNonMatchingEventConsumer getNonMatchingEventConsumer() {
        return nonMatchingEventConsumer;
    }

    public EventRegistryEngineConfiguration setNonMatchingEventConsumer(EventRegistryNonMatchingEventConsumer nonMatchingEventConsumer) {
        this.nonMatchingEventConsumer = nonMatchingEventConsumer;
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

    public DeploymentCache<ChannelDefinitionCacheEntry> getChannelDefinitionCache() {
        return channelDefinitionCache;
    }

    public EventRegistryEngineConfiguration setChannelDefinitionCache(DeploymentCache<ChannelDefinitionCacheEntry> channelDefinitionCache) {
        this.channelDefinitionCache = channelDefinitionCache;
        return this;
    }
    
    public Collection<ChannelModelProcessor> getChannelModelProcessors() {
        return channelModelProcessors;
    }

    public void addChannelModelProcessor(ChannelModelProcessor channelModelProcessor) {
        if (this.channelModelProcessors == null) {
            this.channelModelProcessors = new ArrayList<>();
        }
        this.channelModelProcessors.add(channelModelProcessor);
    }

    public void setChannelModelProcessors(Collection<ChannelModelProcessor> channelModelProcessors) {
        this.channelModelProcessors = channelModelProcessors;
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
    
    public ChannelDefinitionEntityManager getChannelDefinitionEntityManager() {
        return channelDefinitionEntityManager;
    }

    public EventRegistryEngineConfiguration setChannelDefinitionEntityManager(ChannelDefinitionEntityManager channelDefinitionEntityManager) {
        this.channelDefinitionEntityManager = channelDefinitionEntityManager;
        return this;
    }

    public EventResourceEntityManager getResourceEntityManager() {
        return resourceEntityManager;
    }

    public EventRegistryEngineConfiguration setResourceEntityManager(EventResourceEntityManager resourceEntityManager) {
        this.resourceEntityManager = resourceEntityManager;
        return this;
    }

    @Override
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

    public Collection<ELResolver> getPreDefaultELResolvers() {
        return preDefaultELResolvers;
    }

    public EventRegistryEngineConfiguration setPreDefaultELResolvers(Collection<ELResolver> preDefaultELResolvers) {
        this.preDefaultELResolvers = preDefaultELResolvers;
        return this;
    }

    public EventRegistryEngineConfiguration addPreDefaultELResolver(ELResolver elResolver) {
        if (this.preDefaultELResolvers == null) {
            this.preDefaultELResolvers = new ArrayList<>();
        }

        this.preDefaultELResolvers.add(elResolver);
        return this;
    }

    public Collection<ELResolver> getPreBeanELResolvers() {
        return preBeanELResolvers;
    }

    public EventRegistryEngineConfiguration setPreBeanELResolvers(Collection<ELResolver> preBeanELResolvers) {
        this.preBeanELResolvers = preBeanELResolvers;
        return this;
    }

    public EventRegistryEngineConfiguration addPreBeanELResolver(ELResolver elResolver) {
        if (this.preBeanELResolvers == null) {
            this.preBeanELResolvers = new ArrayList<>();
        }

        this.preBeanELResolvers.add(elResolver);
        return this;
    }

    public Collection<ELResolver> getPostDefaultELResolvers() {
        return postDefaultELResolvers;
    }

    public EventRegistryEngineConfiguration setPostDefaultELResolvers(Collection<ELResolver> postDefaultELResolvers) {
        this.postDefaultELResolvers = postDefaultELResolvers;
        return this;
    }

    public EventRegistryEngineConfiguration addPostDefaultELResolver(ELResolver elResolver) {
        if (this.postDefaultELResolvers == null) {
            this.postDefaultELResolvers = new ArrayList<>();
        }

        this.postDefaultELResolvers.add(elResolver);
        return this;
    }

    public EventJsonConverter getEventJsonConverter() {
        return eventJsonConverter;
    }

    public EventRegistryEngineConfiguration setEventJsonConverter(EventJsonConverter eventJsonConverter) {
        this.eventJsonConverter = eventJsonConverter;
        return this;
    }

    public ChannelJsonConverter getChannelJsonConverter() {
        return channelJsonConverter;
    }

    public EventRegistryEngineConfiguration setChannelJsonConverter(ChannelJsonConverter channelJsonConverter) {
        this.channelJsonConverter = channelJsonConverter;
        return this;
    }

    public boolean isEnableEventRegistryChangeDetectionAfterEngineCreate() {
        return enableEventRegistryChangeDetectionAfterEngineCreate;
    }

    public EventRegistryEngineConfiguration setEnableEventRegistryChangeDetectionAfterEngineCreate(boolean enableEventRegistryChangeDetectionAfterEngineCreate) {
        this.enableEventRegistryChangeDetectionAfterEngineCreate = enableEventRegistryChangeDetectionAfterEngineCreate;
        return this;
    }

}
